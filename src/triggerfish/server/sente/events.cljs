(ns triggerfish.server.sente.events
  (:require
   [triggerfish.server.patch :as p]
   [triggerfish.server.object.core :as obj]
   [triggerfish.server.midi :as midi]
   [triggerfish.server.midi-control-adapters :as ctl-adapters]))

;;;; Sente event handlers
(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id) ;; Dispatch on event-id

(defn event-msg-handler
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler :app-state/get
  [{:as ev-msg :keys [ring-req send-fn]}]
  (let [client-id (:client-id (:params ring-req))]
    (send-fn client-id [:patch/recv {:patch (p/get-patch-map)}])
    (send-fn client-id [:fiddled/recv @midi/recently-fiddled])
    (send-fn client-id [:obj-defs/recv (obj/get-public-obj-defs)])))

(defmethod -event-msg-handler :obj/create
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)
        {:keys [name x-pos y-pos]} ?data]
    (p/add-object! name x-pos y-pos)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :obj/move
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/move-object! (:obj-id ?data) (:x-pos ?data) (:y-pos ?data))
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :obj/delete
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (p/remove-object-by-id! (:obj-id ?data))
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :obj/connect
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [{:keys [in-id in-name out-id out-name]} ?data
        session (:session ring-req)
        uid     (:uid     session)]
    (p/connect! in-id in-name out-id out-name)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :obj/disconnect
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [{:keys [in-id in-name]} ?data
        session (:session ring-req)
        uid     (:uid     session)]
    (p/disconnect! in-id in-name)
    (doseq [uid (:any @connected-uids)]
      (send-fn uid [:patch/recv {:patch (p/get-patch-map)}]))))

(defmethod -event-msg-handler :obj/set-control
  [{:as ev-msg :keys [?data ring-req send-fn connected-uids]}]
  (let [session   (:session ring-req)
        uid       (:uid     session)
        [obj-id ctl-name val] ?data]
    (p/set-control! obj-id ctl-name val)
    (doseq [uid (filter #(not= % (:client-id (:params ring-req))) (:any @connected-uids))]
      (send-fn uid [:control/recv [obj-id ctl-name val]]))))

;;TODO this shouldn't be in a random namespace called events it doesn't really make sense, nor does my naming convention for these endpoints (why :patch/ ?)
;;nor does dissocing all of these keys that I don't need
(defmethod -event-msg-handler :patch/subscribe-midi
  [{:keys [?data send-fn connected-uids]}]
  (let [[obj-id ctl-name port-name status-type channel first-data-byte] ?data
        {:as ctl :keys [type params]} (get-in @p/patch [obj-id :controls ctl-name])
        adapter (partial (ctl-adapters/adapters type) params)
        cb (fn [val]
             (let [adapted-val (adapter val)]
               (p/set-control! obj-id ctl-name adapted-val)
               (doseq [uid (:any (deref connected-uids))]
                 (send-fn uid [:control/recv [obj-id ctl-name adapted-val]]))))]
    (midi/subscribe cb port-name status-type channel first-data-byte)))
