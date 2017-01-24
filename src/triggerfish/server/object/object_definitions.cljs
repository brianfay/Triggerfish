(ns triggerfish.server.object.object-definitions
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.object.core]
   [triggerfish.server.id-allocator :as id-alloc])
  (:require-macros
   [triggerfish.server.object.macros :refer
    [constructor destructor control
     outlet-ar outlet-kr inlet-ar inlet-kr
     defobject]]))

(defn synth [synthdef obj-id args]
  "Default method of adding a synth - by putting it at the head of the default group"
  (sc/add-synth-to-head synthdef obj-id sc/default-group args))

(defobject saw
  (constructor
   [freq (rand-int 1000)]
   (synth "saw" obj-id ["freq" freq "out" c/junk-audio-bus]))
  (destructor
   []
   (sc/free-node obj-id))
  (control :freq 220
           [freq] ;;imports
           (sc/set-control obj-id "freq" val)
           [:freq val])    ;;exports
  (inlet-kr :freq
    [freq]
    :connect    (fn [bus] (sc/map-control-to-bus obj-id "freq" bus))
    :disconnect (fn [] (sc/set-control obj-id "freq" freq)))
  (outlet-ar :out
    []
    :connect    (fn [bus] (sc/set-control obj-id "out" bus))
    :disconnect (fn [] (sc/set-control obj-id "out" c/junk-audio-bus))))

(defobject dac
  (constructor
   []
   (synth "stereo-dac" obj-id ["inL" c/silent-audio-bus "inR" c/silent-audio-bus "outL" c/junk-audio-bus "outR" c/junk-audio-bus]))
  (destructor
   []
   (sc/free-node obj-id))
  (inlet-ar :inL
    []
    :connect    (fn [bus] (sc/set-control obj-id "inL" bus)
                          (sc/set-control obj-id "outL" 0))
    :disconnect (fn [] (sc/set-control obj-id "inL"  c/silent-audio-bus)
                       (sc/set-control obj-id "outL" c/junk-audio-bus)))
  (inlet-ar :inR
    []
    :connect    (fn [bus] (sc/set-control obj-id "inR" bus)
                          (sc/set-control obj-id "outR" 1))
    :disconnect (fn [] (sc/set-control obj-id "inR"    c/silent-audio-bus)
                       (sc/set-control obj-id "outR"   c/junk-audio-bus))))
