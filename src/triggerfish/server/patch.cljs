(ns triggerfish.server.patch
  (:require
   [com.stuartsierra.dependency :as dep]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [triggerfish.shared.object-definitions :as obj-def]))

(def id-counter
  (let [counter (atom 1)]
    #(swap! counter inc)))

(defn new-id
  "Returns a unique identifier that can be used as the supercollider node id for an object. 0 and 1 are reserved for the root node and the default group.
  No logic is in place to reuse ids that have become free, but it would take a lot of iterations for the integer to overflow..."
  []
  (id-counter))

;; Describes current Triggerfish objects and their connections. In JVM Clojure world it would probably be preferable to make patch, dag, and sdag refs and have them change together in a transaction, but since js is single-threaded, STM stuff hasn't been implemented for cljs... we don't need to worry about thread safety if there are no threads
(defonce patch (atom {}))

;; Dependency graph derived from the patch.
(def dag (atom {}))


;; Ordered sequence of objects, obtained from a topological sort of the dependency graph
(def sdag)

;; Table storing index of bus currently in use, followed by the last place (index in sorted DAG) in which the bus is used. If 
(def audio-bus-table)

(defn patch->dag
  [patch])

(defn dag->sdag
  [dag])

(defn merge-patch
  "Returns a list of actions needed to transform the given old patch into the given new patch."
  [old-patch new-patch])

(defn merge-sdag
  "Returns a list of actions needed to transform the given old sequence into a given new sequence."
  [old-sdag new-sdag])

(defn make-connections
  "Returns a list of actions needed to make the connections in a given patch"
  [patch])

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
