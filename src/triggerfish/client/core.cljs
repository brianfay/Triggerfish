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

(def initial-state
  {:patch nil
   :positions nil
   })

(register-handler
 :initialize
 (fn
   [db _]
   (merge db initial-state)))

(register-handler
 :patch-recv
 (fn [db [ev-id patch]]
   (assoc db :patch patch)))

;;Updates the app-db with the position of an inlet or outlet
(register-handler
 :update-position
 (fn [db [ev-id obj-id inlet-or-outlet-name pos]]
   (assoc-in db [:positions [obj-id inlet-or-outlet-name]] pos)))

(register-sub
 :patch
 (fn
   [db _]
   (reaction (:patch @db))))

(register-sub
 :positions
 (fn
   [db _]
   (reaction (:positions @db))))

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

(defn outlet-component
  [name]
  [:div {:class "outlet" :ref name}
   name])

(defn inlet-component
  [name]
  [:div {:class "inlet" :ref name}
   name])

(defn get-ref-node
  [this ref]
  (.findDOMNode js/React
                (goog.object/get (.-refs this) ref)))

(defn get-bounding-rect
  [node]
  (let [rect (.getBoundingClientRect node)]
    {:bottom (.-bottom rect)
     :top (.-top rect)
     :left (.-left rect)
     :right (.-right rect)}))

(defn object-component
  "Component for a Triggerfish Object."
  [[id obj-map]]
  (let [x-pos (:x-pos obj-map)
        y-pos (:y-pos obj-map)]
    (reagent/create-class
     {
      :component-did-mount
      (fn [this] 
        (doall
         (map
          (fn
            [name]
            (dispatch [:update-position id name
                            (get-bounding-rect (get-ref-node this (first (keys (:inlets obj-map)))))]))
          (keys (merge (:inlets obj-map) (:outlets obj-map))))))

      :reagent-render
      (fn [[id obj-map]]
        [:div {:class "object"
               :style {:left x-pos
                       :top  y-pos}}
         [:div (str (:name obj-map))]
         (map #(with-meta (inlet-component %) {:key (str id %)}) (keys (:inlets obj-map)))
         (map #(with-meta (outlet-component %) {:key (str id %)}) (keys (:outlets obj-map)))])})))

(defn patch-cable
  [conn positions]
  (let [[[in-id inlet-name] [out-id outlet-name]] conn
        x1 (:left (get positions [in-id inlet-name]))
        y1 (:bottom (get positions [in-id inlet-name]))
        x2 (:right (get positions [out-id outlet-name]))
        y2 (:bottom (get positions [out-id outlet-name]))]
    (fn []
      [:line {:stroke "black"
              :stroke-width 1
              :x1 x1 :y1 y1
              :x2 x2 :y2 y2}])))

;; (defn build-obj-positions
;;   [accum [obj-name obj-map]]
;;    (assoc accum obj-name (select-keys obj-map [:x-pos :y-pos])))

(defn patch-component
  []
  (let [patch (subscribe [:patch])
        objects (reaction (dissoc @patch :connections :positions))
        ;; object-positions (reaction (reduce build-obj-positions {} @objects))
        positions (subscribe [:positions])
        connections (reaction (:connections @patch))]
    (fn
      []
      [:div {:id "patch"}
       (map (fn [obj]
              (with-meta [object-component obj]
                {:key (first obj)})) @objects)
       [:svg {:class "line-box"}
        [:g (doall (map (fn [conn]
                    (with-meta [patch-cable conn @positions]
                      {:key conn})) @connections))]]])))

(defn app
  []
  [:div
   [patch-component]])

(reagent/render [app]
                (js/document.getElementById "app"))

;;;; Init stuff
(defn start! []
  (dispatch-sync [:initialize])
  (start-router!))

;; (defonce _start-once (start!))
(start!)
