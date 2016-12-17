(ns triggerfish.server.patch
  (:require
   [com.stuartsierra.dependency :as dep]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [triggerfish.shared.object-definitions :as obj-def]
   [triggerfish.shared.constants :as c]))

(declare connect! disconnect!)

;; Describes current Triggerfish objects and their connections.
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
  ([n] (when (< n c/silent-audio-bus) (lazy-seq (cons n (private-audio-buses (inc n)))))))

(defn private-control-buses
  "Lazy sequence of usable control buses (it's just positive numbers)."
  ([] (private-control-buses 0))
  ([n] (when (< n c/junk-control-bus) (lazy-seq (cons n (private-control-buses (inc n)))))))

;;The reserved bus map looks like {[type bus] position}, where position is the last inlet where the bus was used (lowest in dependency graph so far)
;;An outlet can reuse a bus if the last inlet where the bus was used is higher than the position of the outlet
(defn reserve-bus
  "Gets a bus that the given connection can use, given the connection type, numbered sorted-dag, and map of reserved buses.
  Tries to reuse a previously used bus if possible."
  [type connection numbered-sdag reserved]
  (let [[[inlet-obj-id] [outlet-obj-id]] connection
        out-pos (get numbered-sdag outlet-obj-id)
        ;;first try to get a previously used bus (if the outlet is lower than a previously used inlet, the bus can be reused)
        candidate (some #(let [[[conn-type bus] pos] %]
                           (when (and (= type conn-type) (< pos out-pos)) %)) reserved)]
    (if (seq candidate)
      (let [[[conn-type bus] pos] candidate]
        bus)
      (let [reserved-audio-set (set (map second (keys (filter #(= :audio (first (first %))) reserved))))
            reserved-control-set (set (map second (keys (filter #(= :control (first (first %))) reserved))))]
        ;;find the first non-reserved bus
        (condp = type
          :audio (some #(when (not (contains? reserved-audio-set %)) %) (private-audio-buses))
          :control (some #(when (not (contains? reserved-control-set %)) %) (private-control-buses)))))))

(defn loop-through-outlets
  "Loops through the outlets of a given object and returns any bus connections that should be made."
  [patch obj-id reserved-buses numbered-sdag]
  (loop [p patch
         connections-to-make []
         connections (get-connected-outlets p obj-id)
         reserved reserved-buses]
    (let [connection (first connections)
          [inlet outlet] connection
          [inlet-obj-id inlet-name] inlet
          [outlet-obj-id outlet-name] outlet]
      (if (nil? outlet)
       ;;base case
       [p connections-to-make reserved]
       (let [type (get-in p [outlet-obj-id :outlets outlet-name :type])
             outlet-bus  (get-in p [outlet-obj-id :outlets outlet-name :bus])]
         (if (not (nil? outlet-bus))
           (recur
            p
            (conj connections-to-make [inlet-obj-id :inlet inlet-name outlet-bus])
            (rest connections)
            (assoc reserved [type outlet-bus]
                   (max (get numbered-sdag inlet-obj-id)
                        (get reserved [type outlet-bus]))))
           (let [bus (reserve-bus type connection numbered-sdag reserved)]
             (recur
              (assoc-in p [outlet-obj-id :outlets outlet-name :bus] bus)
              (conj connections-to-make [inlet-obj-id :inlet inlet-name bus] [outlet-obj-id :outlet outlet-name bus])
              (rest connections)
              (assoc reserved [type bus] (get numbered-sdag inlet-obj-id))))))))))

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
              [temp-p conn reserved] (loop-through-outlets p node reserved-buses numbered-sdag)]
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
      (when (not (outlet-connected? new-patch out-id outlet-name))
        (obj/disconnect-outlet! (get new-patch out-id) outlet-name))
      (obj/disconnect-inlet! (get new-patch in-id) inlet-name)
      (update-patch! new-patch))))


(defn add-object!
  "Adds a new object to the patch."
  ([obj-name]
   (add-object! obj-name (rand-int 500) (rand-int 600)))
  ([obj-name x-pos y-pos]
   (let [obj (obj/obj-constructor (assoc (obj-name obj-def/objects) :name obj-name :x-pos x-pos :y-pos y-pos))]
     (swap! patch assoc (:id obj) obj))))

(defn move-object!
  "Adjusts the x and y position of an object"
  [id x-pos y-pos]
  (reset!
    patch
    (-> @patch
        (assoc-in [id :x-pos] x-pos)
        (assoc-in [id :y-pos] y-pos))))

(defn remove-object!
  "Removes object from the server, first disconnecting all inlets and outlets (which will update the patch)"
  [obj]
  (let [id (:id obj)
        inlets-to-disconnect (keys (merge (into {} (get-connected-inlets @patch id)) (into {} (get-connected-outlets @patch id))))]
    (do
      (doall (map #(apply disconnect! %) inlets-to-disconnect))
      (obj/obj-destructor obj)
      (swap! patch dissoc id))))

(defn set-control!
  [obj-id ctrl-name value]
  (obj/set-control! (get @patch obj-id) ctrl-name value)
  (reset! patch (assoc-in @patch [obj-id :controls ctrl-name :value] value)))

(defn remove-object-by-id!
  [id]
  (remove-object! (@patch id)))

(defn kill-patch!
  "Removes all running nodes from the server and resets the patch to an empty atom"
  []
  (doall (map obj/obj-destructor (vals (filter #(not (= (first %) :connections)) @patch))))
  (reset! patch {})
  (reset! dag (dep/graph))
  (reset! sorted-dag []))

(defn get-patch-map
  "Gets the current patch, converting any records to maps."
  []
  (let [patch @patch]
    (reduce (fn [accum key]
              (let [val (get patch key)]
                (if (record? val)
                  (assoc accum key (into {} (seq val)))
                  (assoc accum key val))))
            {} (keys patch))))
