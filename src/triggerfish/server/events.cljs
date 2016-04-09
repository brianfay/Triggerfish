(ns triggerfish.server.events
  (:require
   [triggerfish.server.patch :as p]
   ))

;;;; Sente event handlers
(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

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

;;If a client requests a patch/notify, we send that client an up to date patch (will happen on page-load)
(defmethod -event-msg-handler :patch/notify
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (send-fn (:client-id (:params ring-req)) [:patch/recv {:patch (p/get-patch-map)}])))

(defmethod -event-msg-handler :patch/create-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/add-object! (p/create-object (:name ?data) (:x-pos ?data) (:y-pos ?data)))))

(defmethod -event-msg-handler :patch/move-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/move-object! (:obj-id ?data) (:x-pos ?data) (:y-pos ?data))))

(defmethod -event-msg-handler :patch/delete-object
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/remove-object-by-id! (:obj-id ?data))))

(defmethod -event-msg-handler :patch/connect
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)
        in-id (:in-id ?data)
        in-name (:in-name ?data)
        out-id (:out-id ?data)
        out-name (:out-name ?data)]
    (p/connect! in-id in-name out-id out-name)))

(defmethod -event-msg-handler :patch/disconnect
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)
        in-id (:in-id ?data)
        in-name (:in-name ?data)]
    (p/disconnect! in-id in-name)))
