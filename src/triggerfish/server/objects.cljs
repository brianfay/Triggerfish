(ns triggerfish.server.objects
  (:require
   [triggerfish.server.scsynth :as sc]))

(defprotocol PObject
  "A Triggerfish Object corresponds to one supercollider node (could be a group or a synth).
  Objects can be created or destroyed, and may have inlets and outlets that can be connected."
  (add-to-server! [this] [this controls])
  (connect-inlet! [this inlet-name bus])
  (connect-outlet! [this outlet-name bus])
  (disconnect-inlet! [this inlet-name])
  (disconnect-outlet! [this outlet-name])
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
(defrecord BasicSynth [id synthdef inlets outlets name x-pos y-pos]
  PObject
  (add-to-server! [this]
    (let [default-controls (get-control-val-pair (merge inlets outlets))]
      (sc/add-synth-to-head synthdef id sc/default-group default-controls)))
  (remove-from-server! [this]
    (sc/free-node id))
  (connect-inlet! [this inlet-name bus]
    (let [props (get inlets inlet-name)]
      (if (= (:type props) :audio)
        (sc/set-control id inlet-name bus)
        (sc/map-control-to-bus id inlet-name bus))))
  (connect-outlet! [this outlet-name bus]
    (let [props (get outlets outlet-name)]
      (if (= (:type props) :audio)
        (sc/set-control id outlet-name bus)
        (sc/map-control-to-bus id outlet-name bus))))
  (disconnect-inlet! [this inlet-name]
    (let [inlet-props (get inlets inlet-name)]
      ;;for controls, value is remembered in :value if set. Otherwise, use :default.
      ;;Audio inlets shouldn't have a :value, so they'll go to :default
      (if (not (nil? (:value inlet-props)))
        (sc/set-control id inlet-name (:value inlet-props))
        (sc/set-control id inlet-name (:default inlet-props)))))
  (disconnect-outlet! [this outlet-name]
    (let [outlet-props (get outlets outlet-name)]
      (sc/set-control id outlet-name (:default outlet-props))))
  (set-control! [this name value]
    (sc/set-control id name value)))

;; (->BasicSynth {:id 1000 :inlets [] :outlets [] :synthdef "saw"})

;;A DAC needs to write to the junk bus when it is not connected to anything
(defrecord DAC [id synthdef inlets outputs]
  PObject
  (add-to-server! [this]
    (let [default-controls (get-control-val-pair (merge inlets outputs))]
      (sc/add-synth-to-head synthdef id sc/default-group default-controls)))
  (remove-from-server! [this]
    (sc/free-node id))
  (connect-inlet! [this inlet-name bus]
    (let [props (get inlets inlet-name)
          output-name (clojure.string/replace inlet-name #"in" "out")
          hardware-out (:hardware-out (get outputs output-name))]
      (if (= (:type props) :audio)
        (do (sc/set-control id inlet-name bus) (sc/set-control id output-name hardware-out))
        (sc/map-control-to-bus id inlet-name bus))))
  (disconnect-inlet! [this inlet-name]
    (let [inlet-props (get inlets inlet-name)
          output-name (clojure.string/replace inlet-name #"in" "out")
          output-props (get outputs output-name)]
      (if (= (:type inlet-props) :audio)
        (do (sc/set-control id inlet-name (:default inlet-props)) (sc/set-control id output-name (:default output-props))))))
  (set-control! [this name value]
    (sc/set-control id name value)))
