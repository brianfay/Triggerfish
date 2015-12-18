(ns server.core
  (:require
   [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))
(defonce dgram (nodejs/require "dgram"))
(defonce spawn (.-spawn (nodejs/require "child_process")))
(defonce osc (nodejs/require "osc-min"))

(defonce scsynth
  (let [scsynth (spawn "scsynth" #js["-u" "57110"])]
   (.on (.-stdout scsynth) "data" #(println (str "stdout: " %)))
   (.on (.-stderr scsynth) "data" #(println (str "stderr: " %)))))

(defn create-socket []
  (let [socket (.createSocket dgram "udp4")]
    (do
      (.on socket "listening" #(println "udp is listening"))
      (.on socket "message" (fn [msg, info]
                              (println (str "msg: " msg))))
      (.on socket "error" #(println (str "error: " %)))
      (.on socket "close" #(println (str "closing udp socket " %)))
    socket)))

(defonce udp-socket (create-socket))

;; (.on udp-server "listening" #(defonce address (.address udp-server)))

;; (.on udp-server "message"
;;      (fn [message, remote]
;;        (println (str (.-remote address) ":" (.-port address) "-" message))))

;; (defonce udp-instance (.bind udp-server 57110 "localhost"))

(defn hello
  []
  (println "Hi from nodejs!!"))

(defn call-scsynth [message]
  (.send udp-socket message 0 (count message) 57110 "localhost"
         (fn [err bytes]
            (if (not (= 0 err)) (console.log (str "There was an error: " (.toString err)))
                (do (console.log "UDP messge sent to localhost:57110"))))))

(def app (express))

(. app (use (serve-static "./" #js {:index "index.html"})))

(def -main (fn []
    (doto (.createServer http #(app %1 %2))
        (.listen 3000))))

(set! *main-cli-fn* -main)

;; (call-scsynth "/d_load synthdefs/default.scsyndef")
;; (call-scsynth "/s_new default")
;; (call-scsynth "/notify 1")
;; (call-scsynth "/g_new 1000")

