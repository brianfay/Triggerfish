(ns triggerfish.server.scsynth
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :as a :refer [pub sub unsub chan >! <! put! timeout]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce dgram (nodejs/require "dgram"))

(defonce osc (nodejs/require "osc-min"))

(defonce spawn (.-spawn (nodejs/require "child_process")))

(defonce sc-chan (chan))

(defonce sc-pub (pub sc-chan #(keyword (string/replace (.-address %) "/" ""))))

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
  "Sends an OSC buffer to scsynth over a udp socket, using the given address and arguments."
  (let [msg (array->osc (js-obj "address" addr "args" (clj->js args)))]
    ;; very useful for debugging
    ;; (println "sending msg to scsynth: " (js-obj "address" addr "args" (clj->js args)))
    (.send udp-socket msg 0 (.-length msg) 57110 "localhost"
           (fn [err bytes]
             (if (and err (not (= 0 err))) (println (str "There was an error: " err)))))))

(defn load-synthdefs
  []
  (call-scsynth "/d_loadDir" "~/.local/share/SuperCollider/synthdefs"))

(defn notify
  [arg]
  (do (call-scsynth "/notify" arg)
      true))

(defn print-fail-messages
  "Logs any error messages from scsynth."
  []
  (let [fail_chan (chan)]
    (sub sc-pub :fail fail_chan)
    (go-loop [msg (<! fail_chan)]
      (println "scsynth: " msg))))

(defn do-when-node-added
  "Will execute the callback when the node is successfully added. Stops checking if no message is received after a second"
  [id callback]
  (let [ngo_chan (timeout 1000)]
    (sub sc-pub :n_go ngo_chan)
    (go-loop [msg (<! ngo_chan)]
      (if-not (nil? msg)
        (let [msg-id (goog.object.getValueByKeys msg "args" 0 "value")]
          (if (= id msg-id) (callback)
              (recur (<! ngo_chan))))
        (unsub sc-pub :n_go ngo_chan)))))

(defn do-when-node-removed
  "Will execute the callback when the node is successfully removed. Stops checking if no message is received after a second"
  [id callback]
  (let [nend_chan (timeout 1000)]
    (sub sc-pub :n_end nend_chan)
    (go-loop [msg (<! nend_chan)]
      (if-not (nil? msg)
        (let [msg-id (goog.object.getValueByKeys msg "args" 0 "value")]
          (if (= id msg-id) (callback)
              (recur (<! nend_chan))))
        (unsub sc-pub :n_go nend_chan)))))

(defn add-group
  "Adds a group with the given id, add-action, and target id."
  [id add-action target]
  (do
    (call-scsynth "g_new" id add-action target)))

(defn add-group-to-head
  "Adds a group with the specified id to the head of the group with the target id."
  [id target]
  (add-group id 0 target))

(defn add-group-to-tail
  "Adds a group with the specified id to the tail of the group with the target id."
  [id target]
  (add-group id 1 target))

(defn add-group-before
  "Adds a group with the specified id before the group with the target id."
  [id target]
  (add-group id 2 target))

(defn add-group-after
  "Adds a group with the specified id before the group with the target id."
  [id target]
  (add-group id 3 target))

(defn add-synth
  ([synthdef id add-action target controls]
   (apply call-scsynth "s_new" synthdef id add-action target controls))
  ([synthdef id add-action target]
   (call-scsynth "s_new" synthdef id add-action target)))

(defn add-synth-to-head
  ([synthdef id target controls]
   (add-synth synthdef id 0 target controls))
  ([synthdef id target]
   (add-synth synthdef id 0 target)))

(defn add-synth-to-tail
  ([synthdef id target controls]
   (add-synth synthdef id 1 target controls))
  ([synthdef id target]
   (add-synth synthdef id 1 target)))

(defn add-synth-before
  ([synthdef id target controls]
   (add-synth synthdef id 2 target controls))
  ([synthdef id target]
   (add-synth synthdef id 2 target)))

(defn add-synth-after
  ([synthdef id target controls]
   (add-synth synthdef id 3 target controls))
  ([synthdef id target]
   (add-synth synthdef id 3 target)))

(defn alloc-buffer [num frames channels]
  (call-scsynth "b_alloc" num frames channels))

(defn free-buffer [num]
  (call-scsynth "b_free"))

(defn move-node-before
  [id target]
   (call-scsynth "n_before" id target))

(defn move-node-after
  [id target]
  (call-scsynth "n_after" id target))

(defn order-nodes
  [add-action target node-ids]
  (apply call-scsynth "n_order" add-action target node-ids))

(defn free-node
  [id]
  (call-scsynth "n_free" id))

(defn free-in-group
  [id]
  (call-scsynth "g_freeAll" id))

(defn set-control
  [id name val]
  (call-scsynth "n_set" id name val))

(defn map-control-to-bus
  [id name bus]
  (call-scsynth "n_map" id name bus))

;;Calling this on load to get confirmation messages for each new node.
;;Returning something at the end of the do block ensures that the defonce won't be retried.
(defonce notify-on (do (notify 1) true))
(defonce default-group (do (call-scsynth "g_new" 1 0 0) 1))
