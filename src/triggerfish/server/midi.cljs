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
(defonce subscribers (atom {}))
(defonce recently-fiddled (atom (take 6 (repeat nil))))

(defn get-status-type [status-byte]
  "Tells you the status type, note that note-on may actually mean note-off if velocity is zero"
  (condp > status-byte
    128 :dunno
    144 :note-off
    160 :note-on
    176 :polyphonic-key-pressure
    192 :control-change
    208 :program-change
    224 :channel-pressure
    240 :pitch-bend
    :system-message));;ignoring system messages for now

(def channel-mask 15) ;; 00001111

(defn get-channel [status-byte]
  (+ 1 (bit-and channel-mask status-byte)))

(defn note? [type]
  (or (= type :note-on) (= type :note-off)))

(defn subscribe
  ([callback port-name status-type channel first-data-byte]
   (cond
     (= status-type :pitch-bend)
     (swap! subscribers assoc [port-name status-type channel nil] callback)

     (note? status-type)
     (swap! subscribers assoc [port-name :note channel first-data-byte] callback)

     :default
     (swap! subscribers assoc [port-name status-type channel first-data-byte] callback))))

(defn unsubscribe
  ([port-name status-type channel first-data-byte]
   (cond
     (= status-type :pitch-bend)
     (swap! subscribers dissoc [port-name status-type channel nil])

     (note? status-type)
     (swap! subscribers dissoc [port-name :note channel first-data-byte])

     :default
     (swap! subscribers dissoc [port-name status-type channel first-data-byte]))))

(defn fiddle-midi [port-name status-type channel first-data-byte]
  (let [fiddled @recently-fiddled
        status-type (if (note? status-type) :note status-type)
        first-data-byte (if (= :pitch-bend status-type) nil first-data-byte)
        ctl [port-name status-type channel first-data-byte]]
    (when-not (some #(= ctl %) fiddled)
      (reset! recently-fiddled (cons [port-name status-type channel first-data-byte] (butlast @recently-fiddled))))))

(defonce midi-msg-handler
  (go-loop []
    (let [[port-name delta-time msg] (<! midi-chan)
          status-byte (first msg)
          status-type (get-status-type status-byte)
          channel     (get-channel status-byte)
          first-data-byte (second msg)
          second-data-byte (nth msg 2)]
      (fiddle-midi port-name status-type channel first-data-byte)
      (condp = status-type
        :pitch-bend
        (when-let [fun (get @subscribers [port-name status-type channel nil])] ;;gonna change resolution back to 0-127 cause whatever why not
          ;;16383 / 129 is 127
          ;; (fun (/ (+ (bit-shift-left first-data-byte 7) second-data-byte) 129.0))
          ;;in theory pitch-bend should send 0-16383, my oxygen keyboard is sending 0-127 on the second data-byte,
          ;;and 0 for the first (except at the very top, where it randomly sends 127)
          ;;I think maybe I just won't use pitch bend
          (fun (/ (+ (bit-shift-left first-data-byte 7) second-data-byte) 129.0)))
        :note-on
        (when-let [fun (get @subscribers [port-name :note channel first-data-byte])]
          (fun second-data-byte))
        :note-off ;;just pretend this is same as note on with zero velocity
        (when-let [fun (get @subscribers [port-name :note channel first-data-byte])]
          (fun 0))
        ;;default
        (when-let [fun (get @subscribers [port-name status-type channel first-data-byte])]
          (fun second-data-byte))))
  (recur)))

(comment
  @subscribers
  (subscribe #(println %1) "USB Oxygen 61 28:0" :pitch-bend 8 nil)
  (subscribe #(println %1) "USB Oxygen 61 28:0" :note 8 42)
  (subscribe #(println %1) "USB Oxygen 61 28:0" :control-change 2 74)
  (subscribe #(println %1) "USB Oxygen 61 28:0" :control-change 2 74)
  (second @recently-fiddled)
  (subscribe #(println %) "QUNEO 24:0" :control-change 1 29)
  (subscribe #(println %) "QUNEO 24:0" :control-change 1 30)
  (subscribe #(println %) "QUNEO 24:0" :control-change 1 31)
  (unsubscribe "QUNEO 24:0" :control-change 1 29)
  (unsubscribe "QUNEO 24:0" :control-change 1 30)
  (unsubscribe "QUNEO 24:0" :control-change 1 31)
  )

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
