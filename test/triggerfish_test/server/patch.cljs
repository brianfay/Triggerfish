(ns triggerfish-test.server.patch
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [triggerfish.server.patch :as p]
            [com.stuartsierra.dependency :as dep]))

(def test-obj {:inlets {"cutoff" {:type :control :val 399}
                    "in1" {:type :audio}
                    "in2" {:type :audio :connected ["synth42" "outL"]}
                    "in3" {:type :control :connected ["lfo5" "out2"]}}})

(deftest get-connected-inlets
  (is (contains? (set (p/get-connected-inlets test-obj))
                 ["in2" {:type :audio :connected ["synth42" "outL"]}]
                 ["in3" {:type :control :connected ["lfo5" "out2"]}])))

(deftest get-connected
  (is (contains? (set (p/get-connections test-obj)) ["synth42" "outL"] ["lfo5" "out2"])))

;; [synth1]  [synth2]
;;   \        /
;;   [mysynth]
(def mysynth ["mysynth" {:inlets {0 {:connected ["synth1" 0]}
                                  1 {:connected ["synth2" 0]}}}])

(deftest build-obj-deps-mysynth
  (let [g (p/build-obj-deps (dep/graph) mysynth)]
    (is (and (dep/depends? g "mysynth" "synth1") (dep/depends? g "mysynth" "synth2")))))

 ;;       [synth2]
 ;;       |\
 ;; [synth1]\
 ;;  |    |  \
 ;;  |   [synth3]
 ;;  |
 ;; [synth4]

(def patch {"synth3"
              {:inlets {1 {:connected ["synth2" 0]}}}
            "synth1"
              {:inlets {1 {:connected ["synth2" 0]}}}
            "synth2"
              {:inlets {}}
            "synth4"
              {:inlets {0 {:connected ["synth1" 0]}}}})

(deftest patch->dag-test
  (let [g (p/patch->dag patch)
        topo-sort (dep/topo-sort g)]
    (is (and (dep/depends? g "synth3" "synth2")
             (dep/depends? g "synth1" "synth2")
             (dep/depends? g "synth4" "synth2")
             (not (dep/depends? g "synth4" "synth3"))
             (= (first topo-sort) "synth2")
             (= (second topo-sort) "synth1")))))

(deftest sort-nodes-same-length
  (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
    (let [old ["1" "2" "3" "4" "5"]
         new ["2" "3" "1" "4" "5"]
         actions (p/sort-nodes! old new)]
     (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3")])))))

(deftest sort-nodes-old-longer
  (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
   (let [old ["1" "2" "3" "6" "4" "5"]
         new ["2" "3" "1" "4" "5"]
         actions (p/sort-nodes! old new)]
     (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3")])))))

(deftest sort-nodes-old-shorter
  (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
   (let [old ["1" "2" "6"]
         new ["2" "3" "1" "4" "5"]
         actions (p/sort-nodes! old new)]
     (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3") '(sc/move-node-after "4" "1") '(sc/move-node-after "5" "4")])))))

(deftest number-each
  (is (= (p/number-each ["foo" "obj2" "obj3" "baz"]) {"foo" 0, "obj2" 1, "obj3" 2, "baz" 3})))
