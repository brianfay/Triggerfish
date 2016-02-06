(ns triggerfish-test.server.patch
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [triggerfish.server.patch :as p]
            [com.stuartsierra.dependency :as dep]))

;;       [synth2]
;;       |\
;; [synth1]\
;;  |    |  \
;;  |   [synth3]
;;  |
;; [synth4]

(def test-patch1 {:connections {
                                ["synth3" "one"] ["synth1" "two"]
                                ["synth3" "two"] ["synth2" "one"]
                                ["synth1" "two"] ["synth2" "one"]
                                ["synth4" "one"] ["synth1" "one"]}})

(deftest get-connected-inlets
  (is (contains? (set (p/get-connected-inlets test-patch1 "synth3"))
                 [["synth3" "one"] ["synth1" "two"]]
                 [["synth3" "two"] ["synth2" "one"]])))

(deftest get-connected-outlets
  (is (contains? (set (p/get-connected-outlets test-patch1 "synth2"))
                 [["synth3" "two"] ["synth2" "one"]]
                 [["synth1" "two"] ["synth2" "one"]])))

;; [synth1]  [synth2]
;;   \        /
;;   [mysynth]
(def conn1 [["mysynth" "one"] ["synth1" "one"]])
(def conn2 [["mysynth" "two"] ["synth2" "two"]])

(deftest build-obj-deps-mysynth
  (let [g (p/build-obj-deps (p/build-obj-deps (dep/graph) conn1) conn2)]
    (is (and (dep/depends? g "mysynth" "synth1") (dep/depends? g "mysynth" "synth2")))))

(deftest patch->dag-test
  (let [g (p/patch->dag test-patch1)
        topo-sort (dep/topo-sort g)]
    (is (and (dep/depends? g "synth4" "synth1")
             (dep/depends? g "synth3" "synth1")
             (dep/depends? g "synth4" "synth1")
             (dep/depends? g "synth1" "synth2")
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
