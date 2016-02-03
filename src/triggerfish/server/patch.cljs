(ns triggerfish.server.patch
  (:require
   [com.stuartsierra.dependency :as dep]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [triggerfish.shared.object-definitions :as obj-def]))

(defonce id-counter
  (let [counter (atom 1)]
    #(swap! counter inc)))

(defn new-id
  "Returns a unique identifier that can be used as the supercollider node id for an object. 0 and 1 are reserved for the root node and the default group.
  No logic is in place to reuse ids that have become free, but it would take a lot of iterations for the integer to overflow..."
  []
  (id-counter))

;; Describes current Triggerfish objects and their connections. In JVM Clojure world it would probably be preferable to make patch, dag, and sorted-dag refs and have them change together in a transaction, but since js is single-threaded, STM stuff hasn't been implemented for cljs... we don't need to worry about thread safety if there are no threads
(defonce patch (atom {}))

;; Dependency graph derived from the patch.
(defonce dag (atom {}))

;; Ordered sequence of objects, obtained from a topological sort of the dependency graph
(defonce sorted-dag (atom {}))

;; Table storing index of bus currently in use, followed by the last place (index in sorted DAG) in which the bus is used. If 
(def audio-bus-table)

;;conn - [inlet-id [out-obj-id outlet-id]]
(defn build-obj-deps
  "Takes a dependency graph and an object key-value pair, and returns a new graph with dependencies from the object to its ancestors."
  [graph obj]
  (let [id (first obj)
        connections (:connections (second obj))]
    (reduce (fn [g conn]
              (let [out-obj-id (first (second conn))]
                (dep/depend g id out-obj-id)))
            graph connections)))

(defn patch->dag
  "Takes a patch object and builds a dependency graph based on the connections of each object in the patch."
  [patch]
  (let [g (dep/graph)]
    (reduce build-obj-deps g patch)))

(defn sort-nodes!
  "Given an old sorted-dag and a new one, returns a vector of the actions needed to sort the nodes (for unit-testing), and also
  performs them as a side-effect."
  [old-sorted-dag new-sorted-dag]
  (loop [actions []
         old-sdag (filterv #(contains? (set new-sorted-dag) %) old-sorted-dag)
         new-sdag new-sorted-dag]
    (cond
      (< (count new-sdag) 2) actions
      (= (second new-sdag) (second old-sdag)) (recur actions (rest old-sdag) (rest new-sdag))
      :else (do (sc/move-node-after (second new-sdag) (first new-sdag))
                (recur (conj actions `(sc/move-node-after ~(second new-sdag) ~(first new-sdag))) (rest old-sdag) (rest new-sdag))))))

(defn update-graph!
  []
  (let [old-dag @dag
        new-dag (patch->dag patch)
        old-sorted-dag @sorted-dag
        new-sorted-dag (dep/topo-sort @dag)]
    (sort-nodes! old-sorted-dag new-sorted-dag)
    ;;TODO: connect buses
    (reset! dag new-dag)
    (reset! sorted-dag new-sorted-dag)))

(defn connect!
  [in-obj-id inlet-idx out-obj-id outlet-idx]
  (when (not (dep/depends? @dag out-obj-id in-obj-id))
    (swap! patch assoc-in [in-obj-id :connections inlet-idx] [out-obj-id outlet-idx])
    (update-graph!)))

(defn create-object
  "Looks up the definition of an object and creates a record. Does not assign an id to the record or add it to the server."
  [obj-name]
  (let [obj-map (obj-name obj-def/objects)
        obj-type (:type obj-map)]
    (condp = obj-type
      :BasicSynth (obj/map->BasicSynth obj-map)
      (println obj-type "is not a valid object type."))))

(defn add-object!
  "Takes a prototype object that has no id. On success, commits the object to the patch."
  [obj-prototype]
  (let [id (new-id)
        obj (assoc obj-prototype :id id)]
    (sc/do-when-node-added id #(swap! patch assoc id obj))
    (obj/add-to-server! obj)))

(defn remove-object!
  "Removes object from the server. On success, removes object from the patch."
  [obj]
  (let [id (:id obj)]
    (sc/do-when-node-removed id #(swap! patch dissoc id))
    (obj/remove-from-server! obj)))
