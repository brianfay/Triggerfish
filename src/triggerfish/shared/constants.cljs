(ns triggerfish.shared.constants)

;;A non-hardware bus that all outlets will write to until connected - set to the last audio bus
(def junk-audio-bus 127)

;;Probably won't need this one, because controls will have static values when disconnected - set to the last control bus
(def junk-control-bus 4095)

(def num-input-bus-channels 8)
(def num-output-bus-channels 8)
(def first-private-audio-bus (+ num-input-bus-channels num-output-bus-channels))
