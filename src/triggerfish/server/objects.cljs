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
  (let [[name val] control
        default (:default val)]
    [name default]))

(defn get-control-val-pair
  "Convenience function that takes a list of controls and returns name value pairs of the controls with defaults."
  [controls]
  (let [control-pairs (map get-ctl-nv (filter #(:default (second %)) controls))]
    (reduce #(into %1 %2) control-pairs)))


;; A BasicSynth is just a Supercollider Synth
(defrecord BasicSynth [id synthdef inlets outlets]
  PObject
  (add-to-server! [this]
    (let [default-controls (get-control-val-pair (merge inlets outlets))]
      (println "default-controls: " default-controls)
      (sc/add-synth-to-head synthdef id sc/default-group default-controls)))
  (remove-from-server! [this]
    (sc/free-node id))
  (connect-inlet! [this name bus]
    (let [props (get inlets name)]
      (if (= (:type props) :audio)
        (sc/set-control id name bus)
        (sc/map-control-to-bus id name bus))))
  (connect-outlet! [this name bus]
    (let [props (get outlets name)]
      (if (= (:type props) :audio)
        (sc/set-control id name bus)
        (sc/map-control-to-bus id name bus))))
  (set-control! [this name value]
    (sc/set-control id name value)))

;; (->BasicSynth {:id 1000 :inlets [] :outlets [] :synthdef "saw"})

;;A DAC needs to write to the junk bus when it is not connected to anything
(defrecord DAC [id synthdef inlets outputs]
  PObject
  (add-to-server! [this]
    (let [default-controls (get-control-val-pair (merge inlets outputs))]
      (println "default-controls: " default-controls)
      (sc/add-synth-to-head synthdef id sc/default-group default-controls)))
  (remove-from-server! [this]
    (sc/free-node id))
  (connect-inlet! [this name bus]
    (let [props (get inlets name)
          output-name (clojure.string/replace name #"in" "out")
          hardware-out (:hardware-out (get outputs output-name))]
      (if (= (:type props) :audio)
        (do (sc/set-control id name bus) (sc/set-control id output-name hardware-out))
        (sc/map-control-to-bus id name bus))))
  (set-control! [this name value]
    (sc/set-control id name value)))
