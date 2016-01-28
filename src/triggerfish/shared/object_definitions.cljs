(ns triggerfish.shared.object-definitions)

(def objects
  {
   :saw
   {
    :type :BasicSynth
    :synthdef "saw"
    :inlets [{:name "freq" :type "control" :value 220}]
    :outlets [{:name "out" :type "audio"}]
   }
   :sine
   {
    :type :BasicSynth
    :synthdef "sine"
    :inlets [{:name "freq" :type "control" :value 220}]
    :outlets [{:name "out" :type "audio"}]
   }
   :lopass
   {
    :type :BasicSynth
    :synthdef "lopass"
    :inlets [{:type "audio" :name "in"} {:type "control" :name "cutoff" :value 1000} {:type "control" :name "res" :value 0.5}]
    :outlets [{:type "audio" :name "out"}]
   }
   :tremolo
   {
    :type :BasicSynth
    :synthdef "tremolo"
    :inlets [{:type "audio" :name "in"} {:type "control" :name "freq" :value 0.5}]
    :outlets [{:type "audio" :name "out"}]
   }
   :dac
   {
    :type :BasicSynth
    :synthdef "dac"
    :inlets [{:type "audio" :name "inL"} {:type "audio" :name "inR"}]
   }
   :adc
   {
    :type :BasicSynth
    :synthdef "adc"
    :outlets [{:type "audio" :name "outL"} {:type "audio" :name "outR"}]
   }})
