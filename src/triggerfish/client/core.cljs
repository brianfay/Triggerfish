(ns triggerfish.client.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [taoensso.sente :as sente :refer [cb-success?]]))

(enable-console-print!)

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

(register-handler
 :patch-recv
 (fn [db [ev-id patch]]
   (assoc db :patch patch)))

(register-sub
 :patch
 (fn
   [db _]
   (reaction (:patch @db))))

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

;; (defmethod -event-msg-handler :chsk/recv
;;   [{:as ev-msg :keys [?data]}]
;;   (println "Push event from server:" ?data))

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

(defn patch-component
  []
  (let [patch (subscribe [:patch])]
    (fn
      []
      [:div
        "PATCH: "
       (let [patch @patch]
         (map (fn [[key val]] ^{:key key} [:div (str key (:name val))]) patch))])))

(defn example
  []
  [:div
   "Hello Brian, I am Triggerfish! It's so nice to meet you!"
   [:button {:id "btn1"
             :on-click (fn [ev]
                         (chsk-send! [:patch/notify {:had-a-callback? "nope"}]))}
    "CLICK ME I TALK TO THE SERVER"]
   [patch-component]])

(reagent/render [example]
                (js/document.getElementById "app"))

;;;; Init stuff
(defn start! [] (start-router!))

(defonce _start-once (start!))
