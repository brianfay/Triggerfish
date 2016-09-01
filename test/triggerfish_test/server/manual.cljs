(ns triggerfish-test.server.manual
  (:require
   [triggerfish.server.patch :as p]
   [triggerfish.server.scsynth :as sc]
   [triggerfish.server.objects :as obj]
   [cljs.core.async :as a :refer [timeout <!]])
   (:require-macros [cljs.core.async.macros :refer [go]]))
;;Informal tests that can be run manually in order to do validation by ear.

(defn connect-test
  "A simple check to see if hooking up two generators to two effects to a DAC all works."
  []
  (go
    ;;2
    (p/add-object! :dac)
    ;;3
    (p/add-object! :sine)
    ;;4
    (p/add-object! :sine)
    ;;5
    (p/add-object! :tremolo)
    ;;6
    (p/add-object! :tremolo)
    ;;It could be possible to try and set control before the id is created, that would be bad, may need a timeout
    ;; (<! (timeout 100))
    (let [cool-jams
          #(do
             (obj/set-control! (get @p/patch 3) "freq" (rand-nth [110 220 440]))
             (obj/set-control! (get @p/patch 4) "freq" (rand-nth [98 196 294 392]))
             (obj/set-control! (get @p/patch 5) "freq" (rand-nth [0.25 0.5 1 2 3]))
             (obj/set-control! (get @p/patch 6) "freq" (rand-nth [0.25 0.5 1 2 3])))]
      (cool-jams)
      (p/connect! 2 "inL" 6 "out")
      (p/connect! 2 "inR" 5 "out")
      (p/connect! 6 "in" 4 "out")
      (p/connect! 5 "in" 3 "out")
      (<! (timeout 500))
      (dotimes [n 10]
        (cool-jams)
        (<! (timeout (/ 1000 n))))
      (println @p/patch)
      (println @p/sorted-dag)
      (p/kill-patch!))))

(defn try-disconnect-inlet!
  []
  (go
    ;;2
    (p/add-object! :dac)
    ;;3
    (p/add-object! :sine)
    (p/connect! 2 "inL" 3 "out")
    (p/connect! 2 "inR" 3 "out")
    (<! (timeout 1000))
    (obj/disconnect-inlet! (get @p/patch 2) "inR")
    (<! (timeout 1000))
    (obj/disconnect-inlet! (get @p/patch 2) "inL")
    (p/kill-patch!)))

(defn try-disconnect-outlet!
  []
  (go
    ;;2
    (p/add-object! :dac)
    ;;3
    (p/add-object! :sine)
    (p/connect! 2 "inL" 3 "out")
    ;; (<! (timeout 1000))
    ;; (obj/disconnect-outlet! (get @p/patch 3) "out")
    (<! (timeout 1000))
    (p/connect! 2 "inL" 3 "out")
    (<! (timeout 500))
    (p/connect! 2 "inR" 3 "out")
    (<! (timeout 500))
    (p/kill-patch!)))

(defn disconnect-test
  "A simple check to see if hooking up two generators to two effects to a DAC all works."
  []
  (go
    ;;2
    (p/add-object! :dac)
    ;;3
    (p/add-object! :sine)
    ;;4
    (p/add-object! :sine)
    ;;5
    (p/add-object! :tremolo)
    ;;6
    (p/add-object! :tremolo)
    ;;It could be possible to try and set control before the id is created, that would be bad, may need a timeout
    ;; (<! (timeout 100))
    (let [cool-jams
          #(do
             (obj/set-control! (get @p/patch 3) "freq" (rand-nth [110 220 440]))
             (obj/set-control! (get @p/patch 4) "freq" (rand-nth [98 196 294 392]))
             (obj/set-control! (get @p/patch 5) "freq" (rand-nth [0.25 0.5 1 2 3]))
             (obj/set-control! (get @p/patch 6) "freq" (rand-nth [0.25 0.5 1 2 3])))]
      (cool-jams)
      (p/connect! 2 "inL" 6 "out")
      (p/connect! 2 "inR" 5 "out")
      (p/connect! 6 "in" 4 "out")
      (p/connect! 5 "in" 3 "out")
      (<! (timeout 500))
      (dotimes [n 10]
        (cool-jams)
        (<! (timeout (/ 1000 n))))
      (p/disconnect! 6 "in")
      (p/disconnect! 5 "in")
      (p/connect! 2 "inL" 3 "out")
      (p/connect! 2 "inR" 4 "out")
      (dotimes [n 10]
        (cool-jams)
        (<! (timeout (/ 1000 n))))
      (println @p/patch)
      (println @p/sorted-dag)
      (p/kill-patch!))))
