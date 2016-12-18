(ns triggerfish.server.events
  (:require
   [triggerfish.server.patch :as p]
   [triggerfish.server.midi :as midi]))

;;;; Sente event handlers
(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id) ;; Dispatch on event-id

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler :app-state/get
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (send-fn (:client-id (:params ring-req)) [:patch/recv {:patch (p/get-patch-map)}])
    (send-fn (:client-id (:params ring-req)) [:fiddled/recv @midi/recently-fiddled])))

(defmethod -event-msg-handler :patch/create-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)
        {:keys [name x-pos y-pos]} ?data]
    (p/add-object! name x-pos y-pos)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :patch/move-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/move-object! (:obj-id ?data) (:x-pos ?data) (:y-pos ?data))
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :patch/delete-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/remove-object-by-id! (:obj-id ?data))
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :patch/connect
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [{:keys [in-id in-name out-id out-name]} ?data
        session (:session ring-req)
        uid     (:uid     session)]
    (p/connect! in-id in-name out-id out-name)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :patch/disconnect
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [{:keys [in-id in-name]} ?data
        session (:session ring-req)
        uid     (:uid     session)]
    (p/disconnect! in-id in-name)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :patch/set-control
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn connected-uids]}]
  (let [session   (:session ring-req)
        uid       (:uid     session)
        obj-id    (:obj-id ?data)
        ctrl-name (:ctrl-name ?data)
        value     (:value ?data)]
    (p/set-control! obj-id ctrl-name value)
    ;;TODO throttle this, or send just the control path/value so it doesn't constantly spam the crap out of other clients with the whole patch
    (doseq [uid (filter #(not= % (:client-id (:params ring-req))) (:any @connected-uids))]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))
