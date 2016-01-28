(ns triggerfish.server.objects
  (:require
   [triggerfish.server.scsynth :as sc]))

(defprotocol PObject
  "A Triggerfish Object corresponds to one supercollider node (could be a group or a synth).
  Objects can be created or destroyed, and may have inlets and outlets that can be connected."
  (add-to-server! [this] [this controls])
  (connect-inlet! [this inlet bus])
  (connect-outlet! [this outlet bus])
  (set-control! [this name val])
  (remove-from-server! [this]))

(defn get-ctl-nv
  [control]
  ((juxt :name :value) control))

(defn get-control-val-pair
  "Convenience function that takes a list of controls and returns name value pairs of the controls with defaults."
  [controls]
  (let [control-pairs (map get-ctl-nv (filter :value controls))]
    (reduce #(into %1 %2) control-pairs)))


;; A BasicSynth is just a Supercollider Synth
(defrecord BasicSynth [id synthdef inlets outlets]
  PObject
  (add-to-server! [this]
    (let [default-controls (get-control-val-pair inlets)]
      (sc/add-synth-to-head synthdef id sc/default-group default-controls)))
  (remove-from-server! [this]
    (sc/free-node id))
  (connect-inlet! [this inlet bus]
    (if (= (:type inlet) "audio")
      (sc/set-control id (:name inlet) bus)
      (sc/map-control-to-bus id (:name inlet) bus)))
  (connect-outlet! [this outlet bus]
    (if (= (:type outlet) "audio")
      (sc/set-control id (:name outlet) bus)
      (sc/map-control-to-bus id (:name outlet) bus)))
  (set-control! [this name value]
    (sc/set-control id name value)))

;; (->BasicSynth {:id 1000 :inlets [] :outlets [] :synthdef "saw"})
