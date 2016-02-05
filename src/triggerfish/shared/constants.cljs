(ns triggerfish.shared.constants)

;;A non-hardware bus that all outlets will write to until connected
(def junk-audio-bus 127)

;;Probably won't need this one, because controls will have static values when disconnected
(def junk-control-bus 4095)

(def num-input-bus-channels 8)
(def num-output-bus-channels 8)
(def first-private-bus (+ num-input-bus-channels num-output-bus-channels))
