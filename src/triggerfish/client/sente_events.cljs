(ns triggerfish.client.sente-events
  (:require [taoensso.sente :as sente :refer [cb-success?]]
            [re-frame.core :refer [dispatch]]))

(let [chsk-type :auto
      packer :edn
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk"
       {:type chsk-type
        :packer packer
        :wrap-recv-evs? false ;;without this server-side pushes go to chsk/recv
        })]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defn request-patch!
  "Sends a request to :patch/notify, which will send out the current patch atom."
  []
  (chsk-send! [:patch/notify]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

;;Event handlers
(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event: " event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  ;;request a patch update when the socket opens
  (when (:first-open? ?data)
    (request-patch!)))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}])

(defmethod -event-msg-handler :patch/recv
  [{:as ev-msg :keys [?data]}]
  (dispatch [:patch-recv (:patch ?data)]))

;;Router
(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)))
