(ns triggerfish.server.objects
  (:require [com.stuartsierra.dependency :as dep]))

;;representation of object graph state - objects with inlets and outlets connected to each other
;;connect outlet 1 of node 2 to inlet 3 of node 5
;; [1 2] [3 5]
;; or maybe:
;;node 1, 2 (outlet 1 connected to node 5 inlet 3), 3, 4, 5 (outlet 1 connected to node 2 inlet 1, outlet 2 connected to node 2 inlet 2) 
;; [1 2 [[1 5 3]] 3 4 5 [[1 2 1] [2 2 2]]]

;;Constructor accepts partial add-action fn, will fill in with own id. Will allocate whatever nodes/buffers needed, returns id of the top-level node

;;0 and 1 reserved for root node and default group.
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

;;later, may want to add a destructor
(defn remove-object
  "Removes object from the server and the node graph"
  [obj])

(def objects
  {
   :saw
   {
    :num-uids 1
    :constructor
    (fn
      [add-fn]
      (let [uid (new-uid)]
        (add-fn "saw" (uid))
        uid))
    :synthdef "saw"
    :inlets [{:type "control" :name "freq"}]
    :outlets [{:type "audio" :name "out"}]
   }
   :sine
   {
    :num-uids 1
    :constructor (fn
                   [id & ids])
    :synthdef "sine"
    :inlets [{:type "control" :name "freq"}]
    :outlets [{:type "audio" :name "out"}]
   }
   :lopass
   {
    :num-uids 1
    :constructor (fn
                   [id & ids])
    :synthdef "lopass"
    :inlets [{:type "audio" :name "in"} {:type "control" :name "cutoff"} {:type "control" :name "res"}]
    :outlets [{:type "audio" :name "out"}]
   }
   :tremolo
   {
    :num-uids 1
    :constructor (fn
                   [id & ids])
    :synthdef "tremolo"
    :inlets [{:type "audio" :name "in"} {:type "control" :name "freq"}]
    :outlets [{:type "audio" :name "out"}]
   }
   :dac
   {
    :num-uids 1
    :constructor (fn
                   [id & ids])
    :synthdef "dac"
    :inlets [{:type "audio" :name "inL"} {:type "audio" :name "inR"}]
   }
   :adc
   {
    :num-uids 1
    :constructor (fn
                   [id & ids])
    :synthdef "adc"
    :outlets [{:type "audio" :name "outL"} {:type "audio" :name "outR"}]
   }
  }
)
;;example object
;; {
;;  :synthdef "filtered-saw"
;;  :inlets [{:type "audio" :name "in"} {:type "control" :name "cutoff"}]
;;  :outlets [{:type "audio" :name "outL"} {:type "audio" :name "outR"}]
;; }

;;use this vector to build DAG

;;use DAG on client-side to check dependencies

;;In Triggerfish, each audio unit is a vertex in a directed acyclic graph (DAG).
;;Each Triggerfish node corresponds to one SuperCollider node (a group or a synth)
;;Triggerfish nodes have predefined inputs and outputs (inlets and outlets)
;;A topological sort of the DAG will determine the ordering of SuperCollider nodes, and buses will be reserved automatically.
;;Whenever possible, buses will be reused.

;;Client state is composed of nodes containing inlets and outlets with either control values or connections, and a list of connections between nodes

;;added node, removed node, created connection, removed connection,
