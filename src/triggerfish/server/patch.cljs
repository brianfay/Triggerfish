(ns triggerfish.server.patch
  (:require
   [com.stuartsierra.dependency :as dep]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [triggerfish.shared.object-definitions :as obj-def]
   [triggerfish.shared.constants :as c]))

(declare connect! disconnect!)

(defonce id-counter
  (let [counter (atom 1)]
    [#(swap! counter inc) #(reset! counter 1)]))

(defonce inc-counter! (first id-counter))

(defonce reset-counter! (second id-counter))

(defn new-id
  "Returns a unique identifier that can be used as the supercollider node id for an object. 0 and 1 are reserved for the root node and the default group.
  No logic is in place to reuse ids that have become free, but it would take a lot of iterations for the integer to overflow..."
  []
  (inc-counter!))

;; Describes current Triggerfish objects and their connections. In JVM Clojure world it would probably be preferable to make patch, dag, and sorted-dag refs and have them change together in a transaction, but since js is single-threaded, STM stuff hasn't been implemented for cljs... we don't need to worry about thread safety if there are no threads
(defonce patch (atom {}))

;; Dependency graph derived from the patch.
(defonce dag (atom (dep/graph)))

;; Ordered sequence of objects, obtained from a topological sort of the dependency graph
(defonce sorted-dag (atom []))

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
  (let [[[inlet-obj-id] [outlet-obj-id]] connection
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
    (if (nil? (first (first inlets)))
      ;;this next line is the base case, anything else will recur
      [p connections-to-make reserved]
      (let [connection (first inlets)
            [[inlet-obj-id inlet-name] [outlet-obj-id outlet-name]] connection
            type (get-in p [outlet-obj-id :outlets outlet-name :type])
            outlet-bus (get-in p [outlet-obj-id :outlets outlet-name :bus])]
        ;;if the outlet already has a bus, just use that. Make sure it is explicitly reserved.
        (if (and (not (nil? outlet-bus))
                 (or
                  (and (= type :audio) (not (= outlet-bus c/junk-audio-bus)))
                  (and (= type :control) (not (= outlet-bus c/junk-control-bus)))))
          (recur
           p
           (conj connections-to-make [inlet-obj-id :inlet inlet-name outlet-bus])
           (rest inlets)
           (assoc reserved [type outlet-bus] (get numbered-sdag inlet-obj-id)))
          (let [bus (reserve-bus type connection numbered-sdag reserved)]
            (recur
             (assoc-in p [outlet-obj-id :outlets outlet-name :bus] bus)
             (conj connections-to-make [inlet-obj-id :inlet inlet-name bus] [outlet-obj-id :outlet outlet-name bus])
             (rest inlets)
             (assoc reserved [type bus] (get numbered-sdag inlet-obj-id)))))))))

(defn get-connection-actions
  "For a given patch and sorted dag, returns an updated patch with bus values set,
  and a vector of the connections that must be made."
  ;;[[node-id :inlet inlet-name bus] [node-id :outlet outlet-name bus] ...etc]
  [init-patch sdag]
  (let [numbered-sdag (number-each sdag)]
    (loop [p init-patch
           nodes sdag
           connections []
           reserved-buses {}]
      (if (nil? (first nodes))
        connections
        (let [node (first nodes)
              [temp-p conn reserved] (loop-through-inlets p node reserved-buses numbered-sdag)]
          (recur
            temp-p
            (rest nodes)
            (apply conj connections conn)
            reserved))))))

(defn build-obj-deps
  "Takes a dependency graph and an object key-value pair, and returns a new graph with dependencies from the object to its ancestors."
  [g conn]
  (let [[[in-id] [out-id]] conn]
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
  (sc/order-nodes 0 sc/default-group new-sorted-dag))
  ;; this is what I did before finding "n_order"
  ;; (loop [actions []
  ;;        old-sdag (filterv #(contains? (set new-sorted-dag) %) old-sorted-dag)
  ;;        new-sdag new-sorted-dag]
  ;;   (cond
  ;;     (< (count new-sdag) 2) actions
  ;;     (= (second new-sdag) (second old-sdag)) (recur actions (rest old-sdag) (rest new-sdag))
  ;;     :else (do (sc/move-node-after (second new-sdag) (first new-sdag))
  ;;               (recur (conj actions `(sc/move-node-after ~(second new-sdag) ~(first new-sdag))) (rest old-sdag) (rest new-sdag))))))

(defn make-connection!
  "Takes an action of form [id :inlet-or-:outlet name bus-number] and  performs the action."
  [p action]
  (let [[id i-or-o name bus] action
        obj (get p id)]
    (condp = i-or-o
      :inlet (obj/connect-inlet! obj name bus)
      :outlet (obj/connect-outlet! obj name bus))))

(defn update-patch!
  "Updates the patch by sorting nodes in order based on their dependencies,
  connecting inlets to outlets by setting them to read and write from shared buses,
  and by updating the state stored in related atoms."
  [new-patch]
  (let [old-dag @dag
        new-dag (patch->dag new-patch)
        old-sorted-dag @sorted-dag
        new-sorted-dag (dep/topo-sort new-dag)]
    (sort-nodes! old-sorted-dag new-sorted-dag)
    (let [actions (get-connection-actions new-patch new-sorted-dag)]
      (doall (map (partial make-connection! new-patch) actions)))
    (reset! patch new-patch)
    (reset! dag new-dag)
    (reset! sorted-dag new-sorted-dag)))

(defn inlet-connected?
  [p in-id inlet-name]
  (contains? (set (keys (:connections p))) [in-id inlet-name]))

(defn outlet-connected?
  [p out-id outlet-name]
  (contains? (set (vals (:connections p))) [out-id outlet-name]))

(defn connect!
  "Connects an inlet to an outlet and forces an update to the patch."
  [in-id inlet-name out-id outlet-name]
  (when (and
         (not (nil? (get-in @patch [in-id :inlets inlet-name])))
         (not (nil? (get-in @patch [out-id :outlets outlet-name])))
         (not (dep/depends? @dag out-id in-id)))
    (when (inlet-connected? @patch in-id inlet-name)
      (disconnect! in-id inlet-name))
    ;;Swap the inlet with the outlet in the :connections map. The inlet is the key, since outlets can be connected to multiple inlets but inlets can only connect to one outlet.
    (update-patch! (assoc-in @patch [:connections [in-id inlet-name]] [out-id outlet-name]))))

(defn disconnect!
  "Disconnects an inlet and forces an update to the patch."
  [in-id inlet-name]
  (if (not (inlet-connected? @patch in-id inlet-name))
    (println "The inlet:" [in-id inlet-name] "is not currently connected.")
    (let [new-patch (update-in @patch [:connections] dissoc [in-id inlet-name])
          outlet (get (:connections @patch) [in-id inlet-name])
          [out-id outlet-name] outlet]
      (when (outlet-connected? new-patch out-id outlet-name)
        (obj/disconnect-outlet! (get new-patch out-id) outlet-name))
      (obj/disconnect-inlet! (get new-patch in-id) inlet-name)
      (update-patch! new-patch))))

(defn create-object
  "Looks up the definition of an object and creates a record. Does not assign an id to the record or add it to the server."
  [obj-name]
  (let [obj-map (obj-name obj-def/objects)
        obj-type (:type obj-map)]
    (condp = obj-type
      :BasicSynth (obj/map->BasicSynth obj-map)
      :DAC (obj/map->DAC obj-map)
      (println obj-type "is not a valid object type."))))

(defn add-object!
  "Takes a prototype object, creates it on the server, and adds it to the patch."
  [obj-prototype]
  (let [id (new-id)
        obj (assoc obj-prototype :id id)]
    (swap! patch assoc id obj)
    ;;this is a pessimistic updating approach
    ;; (sc/do-when-node-added id #(swap! patch assoc id obj))
    (obj/add-to-server! obj)))

(defn remove-object!
  "Removes object from the server, first disconnecting all inlets and outlets (which will update the patch)"
  [obj]
  (let [id (:id obj)
        inlets-to-disconnect (vals (merge (get-connected-inlets @patch id) (get-connected-outlets @patch id)))]
    (println "to disconnect: " inlets-to-disconnect)
    (map #(apply disconnect! %) inlets-to-disconnect)
    (swap! patch dissoc id)
    ;; (sc/do-when-node-removed id #(swap! patch dissoc id))
    (obj/remove-from-server! obj)))

(defn kill-patch!
  "Removes all running nodes from the server and resets the patch to an empty atom"
  []
  (doall (map obj/remove-from-server! (vals (filter #(not (= (first %) :connections)) @patch))))
  (reset! patch {})
  (reset! dag (dep/graph))
  (reset! sorted-dag [])
  (reset-counter!))
