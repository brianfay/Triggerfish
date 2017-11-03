(ns triggerfish.client.views.controls
  (:require [reagent.core :refer [atom create-class dom-node]]
            [re-frame.core :refer [dispatch]]
            [triggerfish.client.utils.helpers :refer [listen]]
            [triggerfish.client.utils.hammer :refer [add-pan]])
  (:require-macros [triggerfish.client.utils.macros :refer [deftouchable]]))

(defonce PI (.-PI js/Math))

(defn draw-dial [canvas min max val]
  (let [ctx (.getContext canvas "2d")
        percent (/ (- val min) (- max min))]
    (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
    (.beginPath ctx)
    (set! (.-lineWidth ctx) 14)
    (set! (.-strokeStyle ctx) "#EEE")
    (.arc ctx 60 60 40 (* 0.5 PI) (* 2.5 PI) false)
    (.stroke ctx)
    (.closePath ctx)
    (.beginPath ctx)
    (set! (.-strokeStyle ctx) "#65ADE2")
    (.arc ctx 60 60 40 (* 0.5 PI) (+ (* 0.5 PI) (* percent (* 2 PI))) false)
    (.stroke ctx)
    (.closePath ctx)
    (set! (.-font ctx) "16px sans-serif")
    (set! (.-textAlign ctx) "center")
    (set! (.-textBaseline ctx) "middle")
    (set! (.-fillStyle ctx) "#EEE")
    (.fillText ctx (.toFixed val 2) 60 60)))

(deftouchable dial-canvas [obj-id ctl-name val]
  (add-pan ham-man
           (fn [ev]
             (let [ev-type (.-type ev)
                   delta-y (.-deltaY ev)]
               (condp = ev-type
                 "panstart"  (dispatch [:start-moving-dial obj-id ctl-name val delta-y])
                 "panend"    (dispatch [:stop-moving-dial obj-id ctl-name delta-y])
                 "pancancel" (dispatch [:stop-moving-dial obj-id ctl-name delta-y])
                 ;;otherwise
                 (dispatch [:move-dial obj-id ctl-name delta-y])))
             (.stopPropagation (.-srcEvent ev))))
  (fn [obj-id ctl-name val] [:canvas {:width "120px" :height "120px"}]))

(defn dial [obj-id ctl-name {:keys [min max]}]
  (let [node (atom nil)]
    (create-class
     {:component-did-mount
      (fn [this] (reset! node (dom-node this))
        (draw-dial (dom-node this) min max (listen [:control-val obj-id ctl-name])))
      :component-did-update
      (fn [this] (draw-dial @node min max (listen [:control-val obj-id ctl-name])))
      :reagent-render
      (fn [obj-id ctl-name params]
        [dial-canvas obj-id ctl-name (listen [:control-val obj-id ctl-name])])})))

(def toggle-style-on {:width            40
                      :height           40
                      :border           "solid 3px"
                      :border-color     "#e33"
                      :background-color "#d22"})

(def toggle-style-off (assoc toggle-style-on :background-color "#433"))

(defn toggle [obj-id ctl-name params]
  (let [val (listen [:control-val obj-id ctl-name])]
    [:div {:style    (if (pos? val) toggle-style-on toggle-style-off)
           :on-click (fn [e]
                       (dispatch [:set-control obj-id ctl-name (* -1 val)]))}
     val]))

(defn control [obj-id ctl-name]
  (let [ctl-params (listen [:control-params obj-id ctl-name])
        {:keys [type] :as params} ctl-params]
    [:div
     [:h3 {:on-click (fn [e] (dispatch [:inspect-control obj-id ctl-name]))} ctl-name]
     (condp = type
       :dial [dial obj-id ctl-name params]
       :toggle [toggle obj-id ctl-name params]
       [:p "unsupported ctl-type: " type])]))
