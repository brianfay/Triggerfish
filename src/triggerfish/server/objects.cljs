(ns triggerfish.server.patch
  (:require [com.stuartsierra.dependency :as dep]
            [triggerfish.server.scsynth :as sc]))

(defprotocol Object
  "A Triggerfish Object corresponds to one supercollider node (could be a group or a synth).
  Objects can be created or destroyed, and may have inlets and outlets that can be connected."
  (create [id])
  (connect-inlet [id inlet-id bus-num])
  (connect-outlet [id outlet-id bus-num])
  (disconnect-inlet [id inlet-id])
  (disconnect-outlet [id outlet-id])
  (destroy [id]))

;;A BasicSynth is just a Supercollider Synth
(defrecord BasicSynth [inlets outlets synthdef]
  (create [id]
    (sc/add-synth-to-head synthdef id sc/default-group))
  (destroy [id]
    (sc/free-node id)))

;; (->BasicSynth {:id 1000 :inlets [] :outlets [] :synthdef "saw"})
