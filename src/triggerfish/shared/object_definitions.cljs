(ns triggerfish.shared.object-definitions
  (:require
   [triggerfish.shared.constants :as c]))

(def objects
  {
   :saw
   {
    :type :BasicSynth
    :synthdef "saw"
    :inlets {
             "freq" {:type :control}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
    :controls {
               "freq" {:nx-type "number" :nx-props {:min 30 :max 10000} :value 220}}
   }
   :sine
   {
    :type :BasicSynth
    :synthdef "sine"
    :inlets {
             "freq" {:type :control}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
    :controls {
               "freq" {:nx-type "number" :nx-props {:min 30 :max 10000} :value 220}}
   }
   :lopass
   {
    :type :BasicSynth
    :synthdef "lopass"
    :inlets {
             "in" {:type :audio :default c/silent-audio-bus}
             "cutoff" {:type :control}
             "res" {:type :control}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
    :controls {
               "cutoff" {:nx-type "number" :min 0 :max 10000 :value 1000}
               "res" {:nx-type "number" :nx-props {:min 0 :max 1} :value 0.5}}
   }
   :tremolo
   {
    :type :BasicSynth
    :synthdef "tremolo"
    :inlets {
             "in" {:type :audio :default c/silent-audio-bus}
             "freq" {:type :control}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
    :controls {
               "freq" {:nx-type "number" :nx-props {:min 0.01 :max 4} :value 0.5}
               }
   }
   :dac
   {
    :type :DAC
    :synthdef "stereo-dac"
    :inlets {
             "inL" {:type :audio :default c/silent-audio-bus}
             "inR" {:type :audio :default c/silent-audio-bus}}
    :outputs {
              "outL" {:type :audio :default c/junk-audio-bus :hardware-out 0}
              "outR" {:type :audio :default c/junk-audio-bus :hardware-out 1}}
   }
   :delay
   {
    :type :BasicSynth
    :synthdef "delay"
    :inlets {
             "in" {:type :audio :default c/silent-audio-bus}
             "delaytime" {:type :control}
             "decaytime" {:type :control}
             }
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}
              }
    :controls {
               "delaytime" {:nx-type "number" :nx-props {:min 0.01 :max 3 :step 0.02} :value 0.5}
               "decaytime" {:nx-type "number" :nx-props {:min 0.01 :max 5 :step 0.02} :value 3}
               }
    }
   :adc
   {
    :type :BasicSynth
    :synthdef "stereo-adc"
    :outlets {
              "outL" {:type :audio :default c/junk-audio-bus}
              "outR" {:type :audio :default c/junk-audio-bus}}
   }})
