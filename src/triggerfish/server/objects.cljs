(ns triggerfish.server.objects
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.id-allocator :as id-alloc])
  )

(comment
  (let [{:keys [constructor controls inlets outlets]} (get @object-registry :saw)]
    (constructor 5))
  (id-alloc/free-obj-id 5)
  (def saw-instance30 (create-object :saw))
  (connect-outlet saw-instance30 :out 0)
  (disconnect-outlet saw-instance30 :out)
  (control-object saw-instance30 :freq 475)

  (:val (first (:controls (get @object-registry :saw))))
  (macroexpand '(defobject saw
     (constructor
      [synth-id (id-alloc/new-node-id) freq 220]
      (sc/add-synth-to-head "saw" synth-id sc/default-group ["freq" freq "out" c/junk-audio-bus]))
     (destructor
      [synth-id]
      (sc/free-node synth-id)
      (id-alloc/free-node-id synth-id))
     (control :freq 220
              [synth-id freq] ;;imports
              (sc/set-control synth-id "freq" val)
              [:freq val])    ;;exports
     (outlet-ar :out
       [synth-id]
       :connect    (fn [bus] (sc/set-control synth-id "out" bus))
       :disconnect (fn [] (sc/set-control synth-id "out" c/junk-audio-bus)))))

  )



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
(defrecord BasicSynth [id synthdef inlets outlets controls name]
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
    (let [inlet-props (get inlets inlet-name)
          controls    (get controls inlet-name)
          type        (:type inlet-props)]
      ;;for controls, value is stored in :value
      ;;Audio inlets shouldn't have a :value, so they'll go to :default from :inlets
      (if (= type :audio)
        (sc/set-control id inlet-name (:default inlet-props))
        (sc/set-control id inlet-name (:value controls)))))
  (disconnect-outlet! [this outlet-name]
    (let [outlet-props (get outlets outlet-name)]
      (sc/set-control id outlet-name (:default outlet-props))))
  (set-control! [this name value]
    (sc/set-control id name value)))

;; (->BasicSynth {:id 1000 :inlets [] :outlets [] :synthdef "saw"})

;;A BufferSynth has one synthdef and needs to allocate a buffer
;;TODO: make ctor which allocates buffer ids so that you don't have to from patch
(defrecord BufferSynth [id synthdef inlets outlets controls name])

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

(defn obj-constructor
  [obj-map]
  (let [obj-type (:type obj-map)
        obj-id   (id-alloc/new-obj-id)
        obj-map  (assoc obj-map :id obj-id)
        obj      (condp = obj-type
                   :BasicSynth
                   (map->BasicSynth obj-map)
                   :DAC
                   (map->DAC obj-map)
                   ;;default
                   (do (id-alloc/free-obj-id obj-id)
                       (throw obj-type " is not a valid object type.")))]
    (add-to-server! obj)
    obj))

(defn obj-destructor
  [obj]
  (remove-from-server! obj)
  (id-alloc/free-obj-id (:id obj)))
