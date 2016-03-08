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
             "freq" {:type :control :default 660}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
   }
   :sine
   {
    :type :BasicSynth
    :synthdef "sine"
    :inlets {
             "freq" {:type :control :default 220}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
   }
   :lopass
   {
    :type :BasicSynth
    :synthdef "lopass"
    :inlets {
             "in" {:type :audio}
             "cutoff" {:type :control :default 1000}
             "res" {:type :control :default 0.5}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
   }
   :tremolo
   {
    :type :BasicSynth
    :synthdef "tremolo"
    :inlets {
             "in" {:type :audio}
             "freq" {:type :control :default 0.5}}
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}}
   }
   :dac
   {
    :type :DAC
    :synthdef "stereo-dac"
    :inlets {
             "inL" {:type :audio :default c/junk-audio-bus}
             "inR" {:type :audio :default c/junk-audio-bus}}
    :outputs {
              "outL" {:type :audio :default c/junk-audio-bus :hardware-out 0}
              "outR" {:type :audio :default c/junk-audio-bus :hardware-out 1}}
   }
   :delay
   {
    :type :BasicSynth
    :synthdef "delay"
    :inlets {
             "in" {:type :audio :default c/junk-audio-bus}
             "delaytime" {:type :control :default 0.2}
             "decaytime" {:type :control :default 1}
             }
    :outlets {
              "out" {:type :audio :default c/junk-audio-bus}
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
