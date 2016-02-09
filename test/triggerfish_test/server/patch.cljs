(ns triggerfish-test.server.patch
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [triggerfish.server.patch :as p]
            [com.stuartsierra.dependency :as dep]
            [triggerfish.shared.constants :as c]))

;;       [synth2]
;;       |\
;; [synth1]\
;;  |    |  \
;;  |   [synth3]
;;  |
;; [synth4]

(def test-patch1 {
                  :connections
                    {["synth3" "one"] ["synth1" "two"]
                    ["synth3" "two"] ["synth2" "one"]
                    ["synth1" "two"] ["synth2" "one"]
                    ["synth4" "one"] ["synth1" "one"]}
                  "synth1"
                    {:inlets {"one" {:type :audio :value c/junk-audio-bus}
                              "two" {:type :audio :value c/junk-audio-bus}}
                     :outlets {"one" {:type :audio :value c/junk-audio-bus}
                               "two" {:type :audio :value c/junk-audio-bus}}}
                  "synth2"
                    {:outlets {"one" {:type :audio :value c/junk-audio-bus}}}
                  "synth3"
                    {:inlets {"one" {:type :audio :value c/junk-audio-bus}
                              "two" {:type :audio :value c/junk-audio-bus}}}
                  "synth4"
                    {:inlets {"one" {:type :audio :value c/junk-audio-bus}}}})

;;same thing but with controls
(def test-patch2 {
                  :connections
                  {["synth3" "one"] ["synth1" "two"]
                   ["synth3" "two"] ["synth2" "one"]
                   ["synth1" "two"] ["synth2" "one"]
                   ["synth4" "one"] ["synth1" "one"]}
                  "synth1"
                  {:inlets {"one" {:type :control :value c/junk-control-bus}
                            "two" {:type :control :value c/junk-control-bus}}
                   :outlets {"one" {:type :control :value c/junk-control-bus}
                             "two" {:type :control :value c/junk-control-bus}}}
                  "synth2"
                  {:outlets {"one" {:type :control :value c/junk-control-bus}}}
                  "synth3"
                  {:inlets {"one" {:type :control :value c/junk-control-bus}
                            "two" {:type :control :value c/junk-control-bus}}}
                  "synth4"
                  {:inlets {"one" {:type :control :value c/junk-control-bus}}}})

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
    (is (dep/depends? g "mysynth" "synth1"))
    (is (dep/depends? g "mysynth" "synth2"))))

(deftest patch->dag-test
  (let [g (p/patch->dag test-patch1)
        topo-sort (dep/topo-sort g)]
    (is (dep/depends? g "synth4" "synth1"))
    (is (dep/depends? g "synth3" "synth1"))
    (is (dep/depends? g "synth4" "synth1"))
    (is (dep/depends? g "synth1" "synth2"))
    (is (not (dep/depends? g "synth4" "synth3")))
    (is (= (first topo-sort) "synth2"))
    (is (= (second topo-sort) "synth1"))))

;;These tests were useful before I discovered n_order
;; (deftest sort-nodes-same-length
;;   (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
;;     (let [old ["1" "2" "3" "4" "5"]
;;          new ["2" "3" "1" "4" "5"]
;;          actions (p/sort-nodes! old new)]
;;      (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3")])))))

;; (deftest sort-nodes-old-longer
;;   (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
;;    (let [old ["1" "2" "3" "6" "4" "5"]
;;          new ["2" "3" "1" "4" "5"]
;;          actions (p/sort-nodes! old new)]
;;      (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3")])))))

;; (deftest sort-nodes-old-shorter
;;   (with-redefs [triggerfish.server.scsynth/call-scsynth #()]
;;    (let [old ["1" "2" "6"]
;;          new ["2" "3" "1" "4" "5"]
;;          actions (p/sort-nodes! old new)]
;;      (is (= actions ['(sc/move-node-after "3" "2") '(sc/move-node-after "1" "3") '(sc/move-node-after "4" "1") '(sc/move-node-after "5" "4")])))))

(deftest number-each
  (is (= (p/number-each ["foo" "obj2" "obj3" "baz"]) {"foo" 0, "obj2" 1, "obj3" 2, "baz" 3})))

(deftest private-audio-buses>hardware
  (is (nil? (some #(< % c/first-private-audio-bus) (p/private-audio-buses)))))

(deftest private-audio-buses<junk-audio-bus
  (is (nil? (some #(> % c/junk-audio-bus) (p/private-audio-buses)))))

(deftest private-control-buses<junk-control-bus
  (is (nil? (some #(> % c/junk-control-bus) (p/private-control-buses)))))

(deftest reserve-audio-bus-no-previously-reserved
  (is (= (p/reserve-bus :audio conn1 {"synth1" 0, "synth2" 1, "mysynth" 2} {}) (first (p/private-audio-buses)))))

(deftest reserve-control-bus-no-previously-reserved
  (is (= (p/reserve-bus :control conn1 {"synth1" 0, "synth2" 1, "mysynth" 2} {}) (first (p/private-control-buses)))))

(deftest reserve-audio-bus-first-three-unavailable
  (is (= (p/reserve-bus :audio conn1 {"synth1" 0, "synth2" 1, "mysynth" 2} {[:audio 16] 0 [:audio 17] 0 [:audio 18] 0 [:control 19] 0}) (+ 3 (first (p/private-audio-buses))))))

(deftest reserve-control-bus-first-three-unavailable
  (is (= (p/reserve-bus :control conn1 {"synth1" 0, "synth2" 1, "mysynth" 2} {[:audio 3] 0 [:control 0] 0 [:control 1] 0 [:control 2] 0}) (+ 3 (first (p/private-control-buses))))))

(deftest reserve-audio-bus-use-available
  ;;The reserved map has an availalbe audio bus at 100, we should use that.
  (is (= (p/reserve-bus :audio [["synth3" "in1"] ["synth4" "out3"]] {"synth1" 0, "synth2" 1, "synth3" 2 "synth4" 3} {[:audio 200] 3 [:audio 100] 0 [:audio 2] 0 [:control 3] 0}) 100)))

(deftest reserve-control-bus-use-available
  ;;The reserved map has an availalbe audio bus at 100, we should use that.
  (is (= (p/reserve-bus :control [["synth3" "in1"] ["synth4" "out3"]] {"synth1" 0, "synth2" 1, "synth3" 2 "synth4" 3} {[:control 200] 3 [:control 100] 0 [:control 2] 0 [:audio 3] 0}) 100)))

(deftest loop-through-audio-inlets-no-reserved
  (is (= (second (p/loop-through-inlets test-patch1 "synth3" {} {"synth2" 0 "synth1" 1 "synth3" 2 "synth4" 3}))
         [["synth3" :inlet "one" (first (p/private-audio-buses))] ["synth1" :outlet "two" (first (p/private-audio-buses))]
          ["synth3" :inlet "two" (second (p/private-audio-buses))] ["synth2" :outlet "one" (second (p/private-audio-buses))]])))

(deftest loop-through-control-inlets-no-reserved
  (is (= (second (p/loop-through-inlets test-patch2 "synth3" {} {"synth2" 0 "synth1" 1 "synth3" 2 "synth4" 3}))
         [["synth3" :inlet "one" (first (p/private-control-buses))] ["synth1" :outlet "two" (first (p/private-control-buses))]
          ["synth3" :inlet "two" (second (p/private-control-buses))] ["synth2" :outlet "one" (second (p/private-control-buses))]])))

(deftest connect-objs
  (let [connections (p/get-connection-actions test-patch1 ["synth2" "synth1" "synth3" "synth4"])]
    ;;are inlets and outlets connected to same buses, as expected?
    (is (= (some #(when (= (butlast %) ["synth3" :inlet "two"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth2" :outlet "one"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth3" :inlet "one"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth1" :outlet "two"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth1" :inlet "two"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth2" :outlet "one"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth1" :inlet "one"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth4" :outlet "one"]) (last %)) connections)))
    ;;there should only be three buses used in these connections
    (is (= (count (set (map last connections))) 3))))

(deftest connect-objs-control
  (let [connections (p/get-connection-actions test-patch2 ["synth2" "synth1" "synth3" "synth4"])]
    ;;are inlets and outlets connected to same buses, as expected?
    (is (= (some #(when (= (butlast %) ["synth3" :inlet "two"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth2" :outlet "one"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth3" :inlet "one"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth1" :outlet "two"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth1" :inlet "two"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth2" :outlet "one"]) (last %)) connections)))

    (is (= (some #(when (= (butlast %) ["synth1" :inlet "one"]) (last %)) connections)
           (some #(when (= (butlast %) ["synth4" :outlet "one"]) (last %)) connections)))
    ;;there should only be three buses used in these connections
    (is (= (count (set (map last connections))) 3))))
