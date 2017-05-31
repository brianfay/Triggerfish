(ns triggerfish.server.object.object-definitions
  (:require
   [triggerfish.server.scsynth :as sc]
   [triggerfish.shared.constants :as c]
   [triggerfish.server.object.core] ;;defobject and associated macros call functions in the core namespace
   [triggerfish.server.id-allocator :as id-alloc]
   [triggerfish.server.object.helpers :refer [simple-constructor simple-destructor
                                              simple-inlet-ar simple-inlet-kr simple-outlet-ar
                                              simple-outlet-kr simple-control synth]])
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

(defobject lf-sine
  (simple-constructor "lfsine")
  (simple-destructor)
  (simple-control :freq {:type :dial :min 0 :max 20} 1)
  (simple-control :mul  {:type :dial :min 0 :max 1000} 100)
  (simple-control :add  {:type :dial :min 0 :max 5000} 200)
  (simple-outlet-kr :out))

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

;;require patch, update-control-val! for controls that need to modify other controls
(defobject loop
  (constructor
   (let [buf-id (id-alloc/new-buffer-id)]
     (sc/alloc-buffer buf-id (* 48000 60) 1)
     ;;LOUSYLOUSYLOUSYBAD hack to get around async buffer creation
     ;;fix ultimately might be to move buffer creation to user, so we can share buffers
     ;;might also listen for /sync response in scsynth.cljs before sending new messages
     (js/setTimeout
      (fn []
        (synth "loop" obj-id ["in" c/silent-audio-bus "bufnum" buf-id "trigger" -1 "rate" 1 "out" c/junk-audio-bus])) 10)))
  (simple-inlet-ar :in)
  (simple-outlet-ar :out)
  (simple-control :trigger {:type :toggle} -1)
  (simple-control :rate    {:type :dial :min -3 :max 3} 1)
  (destructor [buf-id]
   (sc/free-buffer buf-id)
   (sc/free-node obj-id)))

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
