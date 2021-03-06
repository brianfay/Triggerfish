(ns triggerfish.client.views.app
  (:require
   [reagent.core :as reagent :refer [dom-node]]
   [triggerfish.client.utils.hammer :refer [add-pan add-pinch]]
   [triggerfish.client.views.objects :as obj]
   [triggerfish.client.utils.helpers :refer [listen]]
   [triggerfish.client.views.menu :refer [menu]]
   [re-frame.core :refer [dispatch]])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

(defn cable [[[in-obj-id inlet-name] [out-obj-id outlet-name]]]
  (let [[in-x  in-y]   (listen [:inlet-position in-obj-id inlet-name])
        [out-x out-y]  (listen [:outlet-position out-obj-id outlet-name])
        cx1            (+ in-x (/ (- out-x in-x) 2))
        cy1            in-y
        cx2            cx1
        cy2            out-y]
    [:path {:stroke       "#777"
            :fill         "transparent"
            :stroke-width 2
            :d (str "M"  in-x  " " in-y " "
                    "C " cx1   " " cy1  " "
                    cx2   " " cy2  " "
                    out-x " " out-y)}]))

(defn cables []
  "An svg element conaining lines that represent connections between inlets and outlets"
  [:svg
   (map (fn [conn] ^{:key (str "conn: " conn)} [cable conn]) (listen [:connections]))])

(defn patch-canvas []
  "A zero-sized div that shows its contents (overflow visible), acting as a canvas for objects.
Can be translated and scaled for panning/zooming the patch"
  (let [objs (listen [:object-ids])
        pan  (listen [:camera-position])
        zoom (listen [:zoom])
        [x-pos y-pos] pan]
    [:div
     {:style
      {:height    "0px"
       :width     "0px"
       :overflow  "visible"
       :color "#FFF"
       :transform (str "translate3d(" x-pos "px, " y-pos "px, 0px) "
                       "scale("     zoom ", "   zoom ")")}}
     [cables]
     [menu]
     (map (fn [id]
            ^{:key (str  "obj:" id)}
            [obj/object id])
          objs)]))

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
           :on-wheel      (fn [e] (dispatch [:zoom-camera-wheel (.-deltaY e) (.-clientX e) (.-clientY e)]))
           :on-mouse-down (fn [e] (dispatch [:mouse-down-on-canvas (.-clientX e) (.-clientY e)]))
           :on-click      (fn [e] (dispatch [:app-container-clicked (.-clientX e) (.-clientY e)]))}
     [patch-canvas]]))
