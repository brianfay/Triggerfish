(ns triggerfish.server.midi
  (:require
   [cljs.core.async :as a :refer [chan >! <! put!]]
   [cljs.nodejs :as nodejs])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce midi (js/require "midi"))
(defonce Input (.-input midi))
(defonce midi-ports (atom nil))
(defonce used-inputs (atom nil))
(defonce midi-chan (chan))

(defonce midi-msg-handler
  (go-loop []
    (let [[port-name delta-time msg] (<! midi-chan)]
      (println port-name delta-time msg))
    (recur)))

(defonce device-watch
  (add-watch midi-ports
             :midi-port-watch
             (fn [key ref old-state new-state]
               (println "midi changed from " old-state " to: " new-state))))

(defn get-midi-ports
  "Returns midi ports from the given input object"
  [input]
  (reduce (fn [acc i]
            (assoc acc i (.getPortName input i)))
          {}
          (range (.getPortCount input))))

(defn handle-midi-msg-fn [port-name]
  (fn [delta-time msg] (put! midi-chan [port-name delta-time msg])))

(defn setup-input [state]
  ;;close old ports
  (dorun
   (map
    (fn [[input port-idx]]
      (.closePort input)) ;;Kinda dangerous - if this gets run twice for the same input, the program will crash
    @used-inputs))

  ;;open new ports
  (let [new-used-inputs  (reduce
                          (fn [acc [port-idx port-name]]
                            (let [input (Input.)]
                              (.openPort input port-idx)
                              (.on input "message" (handle-midi-msg-fn port-name))
                              (assoc acc input port-idx)))
                          {}
                          state)]
    (reset! used-inputs new-used-inputs)))

(defn poll-ports
  "Checks for new ports at the specified interval, updates on change"
  [interval]
  (let [input (Input.)]
    (js/setInterval (fn []
                      (let [old-state @midi-ports
                            new-state (get-midi-ports input)]
                        (when (not= old-state new-state)
                          (setup-input new-state)
                          (reset! midi-ports (get-midi-ports input)))))
                    interval)))

(defonce port-poller (poll-ports 3000))
