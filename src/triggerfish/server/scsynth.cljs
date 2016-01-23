(ns triggerfish.server.scsynth
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as a :refer [pub sub chan >! <! put!]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(defonce dgram (nodejs/require "dgram"))

(defonce osc (nodejs/require "osc-min"))

(defonce spawn (.-spawn (nodejs/require "child_process")))

(defonce sc-chan (chan))

(defonce sc-pub (pub sc-chan
                     #(do (println (string/replace (.-address %) "/" ""))
                          (keyword (string/replace (.-address %) "/" "")))))

;; (defonce ngo-sub (chan))

;; (sub sc-pub :n_go ngo-sub)

;; (go-loop []
;;   (println "ngo-sub: " (.toString (<! ngo-sub)))
;;   (recur))

(defonce scsynth
  (let [scsynth (spawn "scsynth" #js["-u" "57110"])]
    (.on (.-stdout scsynth) "data" #(println (str "stdout: " %)))
    (.on (.-stderr scsynth) "data" #(println (str "stderr: " %)))))

(defn create-socket []
  (let [socket (.createSocket dgram "udp4")]
    (do
      (.on socket "listening" #(println "udp is listening"))
      (.on socket "message" (fn [msg, info]
                              (go (>! sc-chan (.fromBuffer osc msg)))))
      (.on socket "error" #(println (str "error: " %)))
      (.on socket "close" #(println (str "closing udp socket " %)))
      socket)))

(defonce udp-socket (create-socket))

(defn array->osc [arr]
  (.toBuffer osc arr))

(defn call-scsynth [addr & args]
  (let [msg (array->osc (js-obj "address" addr "args" (clj->js args)))]
    (.send udp-socket msg 0 (.-length msg) 57110 "localhost"
           (fn [err bytes]
             (if (not (= 0 err)) (println (str "There was an error: " (.toString err))))))))

(defn load-synthdefs
  []
  (call-scsynth "/d_loadDir" "~/.local/share/SuperCollider/synthdefs"))

(defn notify
  [arg]
  (call-scsynth "/notify" arg))

;;Calling this on load to get confirmation messages for each new node
(defonce notify-on (notify 1))
