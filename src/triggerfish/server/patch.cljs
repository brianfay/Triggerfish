(ns triggerfish.server.patch
  (:require
   [com.stuartsierra.dependency :as dep]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [triggerfish.shared.object-definitions :as obj-def]
   [triggerfish.shared.constants :as c]))

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

(defn number-each
  "Returns a map where elements of the input vector are the keys and the value is the index that the element appeared in."
  [vec]
  (let [numbers (range (count vec))]
    (apply assoc {} (interleave vec numbers))))

(defn get-connected-inlets
  [patch obj-id]
  (filter #(= obj-id (first (first %))) (:connections patch)))

(defn get-connected-outlets
  [patch obj-id]
  (filter #(= obj-id (first (second %))) (:connections patch)))

(defn private-audio-buses
  "Lazy sequence of usable audio buses."
  ([] (private-audio-buses c/first-private-audio-bus))
  ([n] (when (< n c/junk-audio-bus) (lazy-seq (cons n (private-audio-buses (inc n)))))))

(defn private-control-buses
  "Lazy sequence of usable control buses (it's just positive numbers)."
  ([] (private-control-buses 0))
  ([n] (when (< n c/junk-control-bus) (lazy-seq (cons n (private-control-buses (inc n)))))))

;;The reserved bus map looks like {[type bus] position}, where position is the last inlet where the bus was used (lowest in dependency graph so far)
;;If the objects in the new connection are both lower than the position in the bus map, the bus can be reused (and the position can be updated)
(defn reserve-bus
  "Gets a bus that the given connection can use, given the connection type, numbered sorted-dag, and map of reserved buses.
  Tries to reuse a previously used bus if possible."
  [type connection numbered-sdag reserved]
  (let [inlet (first connection)
        outlet (second connection)
        inlet-obj-id (first inlet)
        outlet-obj-id (first outlet)
        in-pos (get numbered-sdag inlet-obj-id)
        out-pos (get numbered-sdag outlet-obj-id)
        ;;first try to get a previously used bus (if the nodes in the new connection are both after the inlet
        ; where the bus was initially reserved, it can be reused)
        candidate (some #(let [conn-type (first (first %))
                               pos (second %)]
                           (when (and (= type conn-type) (< pos in-pos) (< pos out-pos)) %)) reserved)]
    (if (seq candidate)
      (second (first candidate))
      (let [reserved-audio-set (set (map second (keys (filter #(= :audio (first (first %))) reserved))))
            reserved-control-set (set (map second (keys (filter #(= :control (first (first %))) reserved))))]
        ;;find the first non-reserved bus
        (condp = type
          :audio (some #(when (not (contains? reserved-audio-set %)) %) (private-audio-buses))
          :control (some #(when (not (contains? reserved-control-set %)) %) (private-control-buses)))))))

(defn loop-through-inlets
  "Loops through the inlets of a given object and returns any bus connections that should be made."
  [patch obj-id reserved-buses numbered-sdag]
  (loop [p patch
         connections-to-make []
         inlets (get-connected-inlets p obj-id)
         reserved reserved-buses]
    ;;this next line is the base case, anything else will recur
    (if (nil? (first (first inlets))) [p connections-to-make reserved]
      (let [connection (first inlets)
            inlet (first connection)
            outlet (second connection)
            inlet-obj-id (first inlet)
            inlet-name (second inlet)
            outlet-obj-id (first outlet)
            outlet-name (second outlet)
            type (get-in p [outlet-obj-id :outlets outlet-name :type])
            outlet-bus (get-in p [outlet-obj-id :outlets outlet-name :value])]
        ;;if the outlet already has a bus, just use that. Make sure it is explicitly reserved.
        (if
          (or
            (and (= type :audio) (not (= outlet-bus c/junk-audio-bus)))
            (and (= type :control) (not (= outlet-bus c/junk-control-bus))))
          (recur
            p
            (conj connections-to-make [inlet-obj-id :inlet inlet-name outlet-bus])
            (rest inlets)
            (assoc reserved [type outlet-bus] (get numbered-sdag inlet-obj-id)))
          (let [bus (reserve-bus type connection numbered-sdag reserved)]
            (recur
             (assoc-in p [outlet-obj-id :outlets outlet-name :value] bus)
             (conj connections-to-make [inlet-obj-id :inlet inlet-name bus] [outlet-obj-id :outlet outlet-name bus])
             (rest inlets)
             (assoc reserved [type bus] (get numbered-sdag inlet-obj-id)))))))))

(defn get-connection-actions
  "For a given patch and sorted dag, returns an updated patch with bus values set,
  and a vector of the connections that must be made."
  ;;[[node-id :inlet inlet-name bus] [node-id :outlet outlet-name bus] ...etc]
  [patch sdag]
  (let [numbered-sdag (number-each sdag)]
    (loop [p patch
           nodes sdag
           connections []
           reserved-buses {}]
      (if (nil? (first nodes))
        [patch connections]
        (let [node (first nodes)
              inlet-loop-output (loop-through-inlets p node reserved-buses numbered-sdag)
              temp-p (first inlet-loop-output)
              conn (second inlet-loop-output)
              reserve (second (rest inlet-loop-output))]
          (recur
            temp-p
            (rest nodes)
            (apply conj connections conn)
            (merge reserved-buses reserve)))))))

(defn build-obj-deps
  "Takes a dependency graph and an object key-value pair, and returns a new graph with dependencies from the object to its ancestors."
  [g conn]
  (let [in-id (first (first conn))
        out-id (first (second conn))]
    (dep/depend g in-id out-id)))

(defn patch->dag
  "Takes a patch object and builds a dependency graph based on the connections of each object in the patch."
  [p]
  (let [g (dep/graph)]
    (reduce build-obj-deps g (:connections p))))

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
        new-sorted-dag (dep/topo-sort new-dag)]
    (sort-nodes! old-sorted-dag new-sorted-dag)
    ;; (connect-objs! new-sorted-dag)
    (reset! dag new-dag)
    (reset! sorted-dag new-sorted-dag)))

(defn connect!
  [in-id inlet-name out-id outlet-name]
  (when (not (dep/depends? @dag out-id in-id))
    ;;Swap the inlet with the outlet in the :connections map. The inlet is the key, since outlets can be connected to multiple inlets but inlets can only connect to one outlet.
    (swap! patch assoc-in [:connections [in-id inlet-name]] [out-id outlet-name])
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
