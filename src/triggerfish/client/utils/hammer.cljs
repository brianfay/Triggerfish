(ns triggerfish.client.utils.hammer
  (:require [cljsjs.hammer]))

(defn hammer-manager [elem]
  (new js/Hammer.Manager elem ))

(defn add-tap [ham-man callback]
  (.add ham-man (new js/Hammer.Tap #js {"event" "tap"}))
  (.on ham-man "tap" callback))

(defn add-pan [ham-man callback]
  (.add ham-man (new js/Hammer.Pan #js {"event" "pan"}))
  (.on ham-man "pan" callback))

;; hammerjs documentation mentions that pinch is disabled by default,
;; but this seems to work without using an explicit recognizer on the manager setup
(defn add-pinch [ham-man callback callback-final]
  (.add ham-man (new js/Hammer.Pinch #js {"event" "pinch"}))
  (.on ham-man "pinch" callback)
  (.on ham-man "pinchend" callback-final)
  (.on ham-man "pinchcancel" callback-final))
