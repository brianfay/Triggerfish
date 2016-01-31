(ns triggerfish-test.server.patch
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [triggerfish.server.patch :as p]
            [com.stuartsierra.dependency :as dep]))

;; [synth1]  [synth2]
;;   \        /
;;   [mysynth]
(def connections [[0 ["synth1" 0]] [1 ["synth2" 0]]])
(def mysynth ["mysynth" {:connections connections}])

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
              {:connections [[0 ["synth1" 1]] [1 ["synth2" 0]]]}
            "synth1"
              {:connections [[1 ["synth2" 0]]]}
            "synth2"
              {:connections []}
            "synth4"
              {:connections [[0 ["synth1" 0]]]}})

(deftest patch->dag-test
  (let [g (p/patch->dag patch)
        topo-sort (dep/topo-sort g)]
    (is (and (dep/depends? g "synth3" "synth2")
             (dep/depends? g "synth1" "synth2")
             (dep/depends? g "synth4" "synth2")
             (not (dep/depends? g "synth4" "synth3"))
             (= (first topo-sort) "synth2")
             (= (second topo-sort) "synth1")))))
