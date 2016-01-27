(ns triggerfish.shared.object-definitions)

(def objects {})
;; (def basic-synths
;;   {
;;    :saw
;;    {
;;     :synthdef "saw"
;;     :inlets [{:name "freq" :type "control"}]
;;     :outlets [{:name "out" :type "audio"}] ;;when outlet is connected to something, each control in the list of names will be set to the bus number
;;    }
;;    :sine
;;    {
;;     :synthdef "sine"
;;     :inlets [{:type "control" :name "freq" :default 220}]
;;     :outlets [{:type "audio" :name "out" :default 0}]
;;    }
;;    :lopass
;;    {
;;     :type :synth
;;     :synthdef "lopass"
;;     :inlets [{:type "audio" :name "in" :default 0} {:type "control" :name "cutoff" :default 1000 :min 0 :max 20000} {:type "control" :name "res" :default 0.5}]
;;     :outlets [{:type "audio" :name "out"}]
;;    }
;;    :tremolo
;;    {
;;     :type :synth
;;     :synthdef "tremolo"
;;     :inlets [{:type "audio" :name "in"} {:type "control" :name "freq"}]
;;     :outlets [{:type "audio" :name "out"}]
;;    }
;;    :dac
;;    {
;;     :type :synth
;;     :synthdef "dac"
;;     :inlets [{:type "audio" :name "inL"} {:type "audio" :name "inR"}]
;;    }
;;    :adc
;;    {
;;     :type :synth
;;     :synthdef "adc"
;;     :outlets [{:type "audio" :name "outL"} {:type "audio" :name "outR"}]
;;    }
;;   }
;; )

;;example object
;; {
;;  :synthdef "filtered-saw"
;;  :inlets [{:type "audio" :name "in"} {:type "control" :name "cutoff"}]
;;  :outlets [{:type "audio" :name "outL"} {:type "audio" :name "outR"}]
;; }

;;use this vector to build DAG

;;use DAG on client-side to check dependencies

;;In Triggerfish, each audio unit is a vertex in a directed acyclic graph (DAG).
;;Each Triggerfish node corresponds to one SuperCollider node (a group or a synth)
;;Triggerfish nodes have predefined inputs and outputs (inlets and outlets)
;;A topological sort of the DAG will determine the ordering of SuperCollider nodes, and buses will be reserved automatically.
;;Whenever possible, buses will be reused.

;;Client state is composed of nodes containing inlets and outlets with either control values or connections, and a list of connections between nodes

;;added node, removed node, created connection, removed connection,
