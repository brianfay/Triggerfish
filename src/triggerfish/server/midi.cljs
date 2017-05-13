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
(defonce recently-fiddled (atom {}))

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

;;subscription is a mapping of [port-name status-type channel data-byte] to {[obj-id ctl-name] callback}
;;it's kinda nasty but allows one MIDI control to map to multiple triggerfish controls
(defn subscribe
  ([callback port-name status-type channel first-data-byte obj-id ctl-name]
   (cond
     (= status-type :pitch-bend)
     (swap! subscribers assoc [port-name status-type channel nil] {[obj-id ctl-name] callback})

     (note? status-type)
     (swap! subscribers assoc [port-name :note channel first-data-byte] {[obj-id ctl-name] callback})

     :default
     (swap! subscribers assoc [port-name status-type channel first-data-byte] {[obj-id ctl-name] callback}))))

(defn unsubscribe
  ([port-name status-type channel first-data-byte obj-id ctl-name]
   (cond
     (= status-type :pitch-bend)
     ;; (swap! subscribers dissoc [port-name status-type channel nil])
     (swap! subscribers update-in [[port-name status-type channel nil]] dissoc [obj-id ctl-name])

     (note? status-type)
     (swap! subscribers update-in [[port-name :note channel first-data-byte]] dissoc [obj-id ctl-name])

     :default
     (swap! subscribers update-in [[port-name status-type channel first-data-byte]] dissoc [obj-id ctl-name]))))

(defn serialize-subscribers []
  "The subscribers map without the callback functions (can be sent to the client)"
  (reduce (fn [acc [k v]] (assoc acc k (first (keys v)))) {} @subscribers))

;;loop over each k-v in the subscribers map
;;loop over each subscribed [obj-id ctl-name] pair
;;if the obj-id matches, add it to the list of keys to dissoc
(defn unsubscribe-object [obj-id]
  (let [subs @subscribers
        kvs-to-remove
          (filter identity
            (for [sub @subscribers
                  v (val sub)]
              (when (= (ffirst v) obj-id)
                [(key sub) (first v)])))]
    ;;if the subscriber is the only one for that midi control, dissoc the midi control
    ;;otherwise only dissoc the specific subscriber
    (->> (reduce (fn [acc [k v]] (if (= (count (get acc k)) 1)
                               (dissoc acc k)
                               (update acc k dissoc v)))
                subs kvs-to-remove)
        (reset! subscribers))))

(def max-fiddled-list 6);; maximum number of controls to store in the recently fiddled list (per each device)

(defn fiddle-midi
  "Updates lists of midi controls that have recently been fiddled with."
  [port-name status-type channel first-data-byte]
  (let [fiddled @recently-fiddled
        status-type (if (note? status-type) :note status-type)
        first-data-byte (if (= :pitch-bend status-type) nil first-data-byte)
        ctl [status-type channel first-data-byte]]
    (if-let [fiddled-list (get fiddled port-name)]
      (if (some #(= ctl %) fiddled-list)
        (swap! recently-fiddled assoc port-name (cons ctl (remove #(= ctl %) fiddled-list)));;if this control is already in the recently-fiddled list, move it to the top
        (if (> max-fiddled-list (count fiddled-list))
          (swap! recently-fiddled assoc port-name (cons ctl fiddled-list))
          (swap! recently-fiddled assoc port-name (cons ctl (butlast fiddled-list)))));;if there's too many things in the list, drop the last one
      (swap! recently-fiddled assoc port-name (list ctl)))))

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
        (when-let [subs (get @subscribers [port-name status-type channel nil])] 
          ;;16383 / 129 is 127
          ;; (fun (/ (+ (bit-shift-left first-data-byte 7) second-data-byte) 129.0))
          ;;in theory pitch-bend should send 0-16383, my oxygen keyboard is sending 0-127 on the second data-byte,
          ;;and 0 for the first (except at the very top, where it randomly sends 127)
          ;;I think maybe I just won't use pitch bend
          (let [funs (map second subs)]
            (doseq [fun funs]
              (fun (/ (+ (bit-shift-left first-data-byte 7) second-data-byte) 129.0)))))
        :note-on
        (when-let [subs (get @subscribers [port-name :note channel first-data-byte])]
          (let [funs (map second subs)]
            (doseq [fun funs]
              (fun second-data-byte))))
        :note-off ;;just pretend this is same as note on with zero velocity
        (when-let [subs (get @subscribers [port-name :note channel first-data-byte])]
          (let [funs (map second subs)]
            (doseq [fun funs]
              (fun 0))))
        ;;default
        (when-let [subs (get @subscribers [port-name status-type channel first-data-byte])]
          (let [funs (map second subs)]
            (doseq [fun funs]
              (fun second-data-byte))))))
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
  (fn [delta-time msg]
    (put! midi-chan [port-name delta-time msg])))

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
