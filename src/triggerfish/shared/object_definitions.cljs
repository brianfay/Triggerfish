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
             "freq" {:type :control :value 220}}
    :outlets {
              "out" {:type :audio :value c/junk-audio-bus}}
   }
   :sine
   {
    :type :BasicSynth
    :synthdef "sine"
    :inlets {
             "freq" {:type :control :value 220}}
    :outlets {
              "out" {:type :audio :value c/junk-audio-bus}}
   }
   :lopass
   {
    :type :BasicSynth
    :synthdef "lopass"
    :inlets {
             "in" {:type :audio}
             "cutoff" {:type :control :value 1000}
             "res" {:type :control :value 0.5}}
    :outlets {
              "out" {:type :audio :value c/junk-audio-bus}}
   }
   :tremolo
   {
    :type :BasicSynth
    :synthdef "tremolo"
    :inlets {
             "in" {:type :audio}
             "freq" {:type :control :value 0.5}}
    :outlets {
              "out" {:type :audio :value c/junk-audio-bus}}
   }
   :dac
   {
    :type :BasicSynth
    :synthdef "dac"
    :inlets {
             "inL" {:type :audio :value c/junk-audio-bus}
             "inR" {:type :audio :value c/junk-audio-bus}}
   }
   :adc
   {
    :type :BasicSynth
    :synthdef "adc"
    :outlets {
              "outL" {:type :audio :value c/junk-audio-bus}
              "outR" {:type :audio :value c/junk-audio-bus}}
   }})
