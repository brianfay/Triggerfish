(ns triggerfish.client.views.objects
  (:require
   [reagent.core :refer [atom create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-tap add-pan add-pinch]]
   [re-frame.core :refer [subscribe dispatch]]))

(def obj-retracted-style
  {:min-height "40px"
   :min-width  "70px"})

(def obj-expanded-style
  {:min-height "400px"
   :min-width  "400px"})

(defn obj-display []
  [:div {:style {:opacity 1
                 :transition "opacity 4s ease-in"}}
   [:h1 "super cool object"]
   "informational text"])

;;dragging objects doesn't work very well if pull-down-to-refresh is enabled, you can turn this off manually in chrome://flags
;;but maybe a stopPropagation fix would work?
(defn object [id init-params]
  (let [params   (subscribe [:obj-params id])
        ham      (atom nil)
        expanded (atom false)]
    (create-class
     {:component-did-mount ;;subscribe hammer
      (fn [this]
        (let [ham-man (hammer-manager (dom-node this))]
          ;; (set! (.-domEvents ham-man) true) ;; thought I read this was necessary to call stopPropagation, doesn't seem to be
          (add-tap ham-man (fn [ev] (swap! expanded not)))

          (add-pan ham-man (fn [ev]
                             (.stopPropagation (.-srcEvent ev)) ;; without this, you can pan the camera while moving an object by using two fingers.
                             (let [delta-x (.-deltaX ev)
                                   delta-y (.-deltaY ev)]
                               (if-not (.-isFinal ev)
                                 (dispatch [:offset-object id delta-x delta-y])
                                 (dispatch [:commit-object-position id])))))
          ;;prevent pinch on object to zoom the patch-canvas
          (add-pinch ham-man
                     (fn [ev]
                       (.stopPropagation (.-srcEvent ev)))
                     (fn [ev]
                       (.stopPropagation (.-srcEvent ev))))
          (reset! ham ham-man)))
      :component-will-unmount
      (fn []
        (when-let [ham-man @ham]
          (.destroy ham-man)))
      :reagent-render
      (fn [id]
        (let [{:keys [x-pos y-pos offset-x offset-y]} @params]
          [:div {:class            "object"
                 :style (merge (if @expanded
                                 obj-expanded-style
                                 obj-retracted-style)
                               {:position         "fixed"
                                :left             x-pos
                                :top              y-pos
                                :transform        (str "translate(" offset-x "px, " offset-y "px)")
                                :transition       "min-width 0.25s, min-height 0.25s"})}
           (if @expanded
             [obj-display]
             [:p id])]))})))
