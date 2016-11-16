(ns triggerfish.client.views.app
  (:require
   [reagent.core :as reagent :refer [atom create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-pan add-pinch]]
   [triggerfish.client.views.objects :as obj]
   [re-frame.core :refer [subscribe dispatch]]))

(defn patch-canvas []
  "A zero-sized div that shows its contents (overflow visible), acting as a canvas for objects.
Can be translated and scaled for panning/zooming the patch"
  (let [objs (subscribe [:objects])
        pan  (subscribe [:camera-position])
        zoom (subscribe [:zoom])]
    (fn []
      (let [[x-pos y-pos] @pan]
        [:div
         {:style
          {:height    "0px"
           :width     "0px"
           :overflow  "visible"
           :transform (str "translate(" x-pos "px, " y-pos "px) "
                           "scale("     @zoom ", "   @zoom ")")}}
         (map (fn [id init-params]
                ^{:key (str "obj: " id)}
                [obj/object id init-params])
              @objs)]))))

(defn app-container []
  "This div holds takes up 100% of the screen. Pinch and pan events on this viewport are used to scale and translate the patch-canvas."
  (let [ham (atom nil)]
    (create-class
     {:component-did-mount
      (fn [this]
        (let [dom-node (dom-node this)
              ham-man  (hammer-manager dom-node)]
          (add-pan ham-man
                   (fn [ev]
                     (when (= (.-target ev) dom-node)
                       (if-not (.-isFinal ev)
                         (dispatch [:pan-camera (.-deltaX ev) (.-deltaY ev)])
                         (dispatch [:commit-camera-pan])))))
          (add-pinch ham-man
                     (fn [ev]
                       (dispatch [:zoom-camera (.-scale ev)]))
                     (fn [ev]
                       (dispatch [:commit-camera-zoom])))
          (reset! ham ham-man)))
      :component-will-unmount
      (fn []
        (when-let [ham-man @ham]
          (.destroy ham-man)))
      :reagent-render
      (fn []
        [:div {:style
               {:width  "100%"
                :height "100%"}
               :overflow "hidden"}
         [patch-canvas]])})))
