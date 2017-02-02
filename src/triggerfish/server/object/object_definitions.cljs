(ns triggerfish.server.object.object-definitions
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.object.core] ;;defobject and associated macros call functions in the core namespace
   [triggerfish.server.id-allocator :as id-alloc]
   [triggerfish.server.object.helpers :refer [simple-constructor simple-destructor
                                              simple-inlet-ar simple-inlet-kr simple-outlet-ar
                                              simple-outlet-kr simple-control]])
  (:require-macros
   [triggerfish.server.object.macros :refer [constructor destructor control
                                             outlet-ar outlet-kr inlet-ar inlet-kr
                                             defobject]]))

(defobject saw
  (simple-constructor "saw")
  (simple-destructor)
  (simple-control  :freq {:type :dial :min 50 :max 15000} 220)
  (simple-inlet-kr :freq)
  (simple-outlet-ar :out))

(defobject sine
  (simple-constructor "sine")
  (simple-destructor)
  (simple-control :freq {:type :dial :min 50 :max 15000} 220)
  (simple-inlet-kr :freq)
  (simple-outlet-ar :out))

(defobject adc
  (simple-constructor "stereo-adc")
  (simple-destructor)
  (simple-outlet-ar :outL)
  (simple-outlet-ar :outR))


(defobject delay
  (simple-constructor "delay")
  (simple-destructor)
  (simple-outlet-ar :out)
  (simple-inlet-ar  :in)
  (simple-control   :delaytime {:type :dial :min 0.1 :max 5} 0.3)
  (simple-control   :decaytime {:type :dial :min 0.1 :max 5} 2)
  (simple-inlet-kr  :delaytime)
  (simple-inlet-kr  :decaytime))

(defobject dac
  (simple-constructor "stereo-dac")
  (simple-destructor)
  (inlet-ar :inL
    :connect    (fn [bus] (sc/set-control obj-id "inL" bus)
                  (sc/set-control obj-id "outL" 0))
    :disconnect (fn [] (sc/set-control obj-id "inL"  c/silent-audio-bus)
                  (sc/set-control obj-id "outL" c/junk-audio-bus)))
  (inlet-ar :inR
    :connect    (fn [bus] (sc/set-control obj-id "inR" bus)
                  (sc/set-control obj-id "outR" 1))
    :disconnect (fn [] (sc/set-control obj-id "inR"    c/silent-audio-bus)
                  (sc/set-control obj-id "outR"   c/junk-audio-bus))))
