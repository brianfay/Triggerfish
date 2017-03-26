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
        :wrap-recv-evs? false})] ;;without this server-side pushes go to chsk/recv
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defn request-app-state!
  "Sends a request to :app-state/get, which will send out the current patch atom and any other needed app state."
  []
  (chsk-send! [:app-state/get]))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

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
  [{:keys [?data] :as thing}]
  ;;request a patch update when the socket opens
  (when (:first-open? (second ?data)) ;;?data is [old-state new-state]
    (request-app-state!)))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}])

(defmethod -event-msg-handler :patch/recv
  [{:as ev-msg :keys [?data]}]
  (def data ?data)
  (dispatch [:patch-recv (:patch ?data)]))

(defmethod -event-msg-handler :fiddled/recv
  [{:as ev-msg :keys [?data]}]
  (dispatch [:midi/fiddled ?data]))

(defmethod -event-msg-handler :obj-defs/recv
  [{:as ev-msg :keys [?data]}]
  (dispatch [:obj-defs ?data]))

(defmethod -event-msg-handler :control/recv
  [{:as ev-msg :keys [?data]}]
   (dispatch [:update-control ?data]))

;;Router
(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)))
