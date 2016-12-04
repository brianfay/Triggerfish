(ns triggerfish.client.views.app
  (:require
   [reagent.core :as reagent :refer [atom create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-pan add-pinch]]
   [triggerfish.client.views.objects :as obj]
   [triggerfish.client.views.menu :refer [menu]]
   [re-frame.core :refer [subscribe dispatch]])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

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
           :color "#FFF"
           :transform (str "translate3d(" x-pos "px, " y-pos "px, 0px) "
                           "scale("     @zoom ", "   @zoom ")")}}
         (map (fn [[id _]]
                [obj/object id])
              @objs)]))))

(deftouchable app-container []
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
  (fn []
    [:div {:style
           {:width  "100%"
            :height "100%"}
           :overflow "hidden"
           :on-click (fn [e] (dispatch [:app-container-clicked (.-clientX e) (.-clientY e)]))}
     [menu]
     [patch-canvas]]))
