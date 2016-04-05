(ns triggerfish-test.server.objects
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [triggerfish.server.objects :as obj]
            [triggerfish.shared.constants :as c]
            [triggerfish.shared.object-definitions :as obj-def]))

(deftest get-ctl-nv
  (is (= (obj/get-ctl-nv ["saw" {:default c/junk-audio-bus}]) ["saw" c/junk-audio-bus])))

(deftest get-control-val-pair
  (is (= (obj/get-control-val-pair (:inlets (:sine obj-def/objects))) ["freq" 220]))
  (is (= (obj/get-control-val-pair (:inlets (:lopass obj-def/objects))) ["in" 126 "cutoff" 1000 "res" 0.5])))
