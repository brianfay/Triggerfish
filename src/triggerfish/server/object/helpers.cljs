(ns triggerfish.server.object.helpers
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.object.core]) ;;defobject and associated macros call functions in the core namespace
  (:require-macros
   [triggerfish.server.object.macros :refer
    [constructor destructor control
     outlet-ar outlet-kr inlet-ar inlet-kr
     defobject]]))

(defn synth [synthdef obj-id args]
  "Default method of adding a synth - by putting it at the head of the default group"
  (sc/add-synth-to-head synthdef obj-id sc/default-group args))

(defn simple-constructor [synthdef]
  "A not so simple function that generates a simple constructor.
Expects that we're working with one synthdef, and that controls, inlets and outlets are set up correctly in the defobject."
  (constructor
   (let [{:keys [controls inlets outlets]} obj-map
         ctl-names (map name (keys controls))
         ctl-vals  (map :val (vals controls))
         ctl-vec   (interleave ctl-names ctl-vals)

         ;;control inlets don't read from anything by default
         ;;inlet-krs
         inlet-ars (filter (fn [[k v]] (= (:type v) :audio)) inlets)
         inlet-ar-names (map name (keys inlet-ars))
         inlet-ar-vec (interleave inlet-ar-names (repeat (count inlet-ar-names) c/silent-audio-bus))

         outlet-krs (filter (fn [[k v]] (= (:type v) :control)) outlets)
         outlet-kr-names (map name (keys outlet-krs))
         outlet-kr-vec (interleave outlet-kr-names (repeat (count outlet-kr-names) c/junk-control-bus))

         outlet-ars (filter (fn [[k v]] (= (:type v) :audio)) outlets)
         outlet-ar-names (map name (keys outlet-ars))
         outlet-ar-vec (interleave outlet-ar-names (repeat (count outlet-ar-names) c/junk-audio-bus))]
     (synth synthdef obj-id (vec (concat ctl-vec inlet-ar-vec outlet-kr-vec outlet-ar-vec))))))

(defn simple-destructor []
  "A basic destructor that does nothing but free the node associated with the obj-id."
  (destructor (sc/free-node obj-id)))

(defn simple-control [ctl-name type init-ctl-val]
  "A basic control that sets the control on the associated obj-id to the provided val."
  (control ctl-name type init-ctl-val
    (sc/set-control obj-id (name ctl-name) val)))

(defn simple-inlet-kr [inlet-name]
  "A basic control inlet. On connect, maps the control to read from the bus. On disconnect, returns to the
last set value for the associated control."
  (inlet-kr inlet-name
    :connect    (fn [bus] (sc/map-control-to-bus obj-id (name inlet-name) bus))
    :disconnect (fn []    (sc/set-control obj-id (name inlet-name) (get-in obj-map [:controls inlet-name :val])))))

(defn simple-inlet-ar [inlet-name]
  "A basic audio inlet. On connect, sets the control to read from the provided bus. On disconnect, reads from the silent audio bus."
  (inlet-ar inlet-name
    :connect    (fn [bus] (sc/set-control obj-id (name inlet-name) bus))
    :disconnect (fn [] (sc/set-control obj-id (name inlet-name) c/silent-audio-bus))))

(defn simple-outlet-ar [outlet-name]
  "A basic audio outlet. On connect, sets the control to write to the provided bus. On disconnect, writes to the junk audio bus."
  (outlet-ar outlet-name
    :connect    (fn [bus] (sc/set-control obj-id (name outlet-name) bus))
    :disconnect (fn [] (sc/set-control obj-id (name outlet-name) c/junk-audio-bus))))

(defn simple-outlet-kr [outlet-name]
  "A basic control outlet. On connect, sets the control to write to the provided bus. On disconnect, writes to the junk audio bus."
  (outlet-kr outlet-name
    :connect    (fn [bus] (sc/set-control obj-id (name outlet-name) bus))
    :disconnect (fn [] (sc/set-control obj-id (name outlet-name) c/junk-control-bus))))
