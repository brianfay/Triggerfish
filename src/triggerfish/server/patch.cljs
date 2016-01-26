(ns triggerfish.server.patch
  (:require [com.stuartsierra.dependency :as dep]
            [triggerfish.server.scsynth :as sc]
            [triggerfish.shared.objects :refer [objects]]))

(def id-counter
  (let [counter (atom 1)]
    #(swap! counter inc)))

(defn new-uid
  "Returns a unique identifier that can be used as the supercollider node id for an object.
  No logic is in place to reuse ids that have become free, but it would take a lot of iterations for the integer to overflow..."
  []
  (id-counter))

;;Describes current Triggerfish objects and their connections
(def patch)

;;Dependency graph derived from the patch
(def dag)

;;Ordered sequence of objects, obtained from a topological sort of the dependency graph
(def sdag)

;;Table storing index of bus currently in use, followed by the last place (index in sorted DAG) in which the bus is used. If 
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

(defn add-object
  "Adds an object to the server. When the parent node of the object is successfully added, the object will be added to the patch atom. Params will be associated with the object"
  [obj-type params add-action target]
  (let [uid (new-uid)
        obj (get objects type)
        constructor (get obj :constructor)]
    (sc/do-when-node-added
        (fn []
          (swap! patch
                 #(assoc % uid
                         {:type type
                          :inlets (get obj :inlets)
                          :outlets (get obj :outlets)})))
      new-uid)
    (constructor uid)))

(defn remove-object
  "Removes object from the server. On success, removes object from the patch."
  [obj])
