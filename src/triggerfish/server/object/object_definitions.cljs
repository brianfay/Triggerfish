(ns triggerfish.server.object.object-definitions
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.id-allocator :as id-alloc])
  (:require-macros
   [triggerfish.server.object.macros :refer
    [constructor destructor control
     outlet-ar outlet-kr inlet-ar inlet-kr
     defobject]]))

(defobject saw
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
  (inlet-kr :freq
    [synth-id freq]
    :connect    (fn [bus] (sc/map-control-to-bus synth-id "freq" bus))
    :disconnect (fn [] (sc/set-control synth-id "freq" freq)))
  (outlet-ar :out
    [synth-id]
    :connect    (fn [bus] (sc/set-control synth-id "out" bus))
    :disconnect (fn [] (sc/set-control synth-id "out" c/junk-audio-bus))))
