(ns triggerfish.client.views.objects
  (:require
   [reagent.core :refer [atom create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-tap add-pan add-pinch]]
   [re-frame.core :refer [subscribe dispatch]])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

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

(deftouchable object-impl [id init-params params expanded]
  (add-tap ham-man (fn [ev] (swap! expanded not)))
  (add-pan ham-man
           (fn [ev]
             (.stopPropagation (.-srcEvent ev)) ;; without this, you can pan the camera while moving an object by using two fingers.
             (let [delta-x (.-deltaX ev)
                   delta-y (.-deltaY ev)]
               (if-not (.-isFinal ev)
                 (dispatch [:offset-object id delta-x delta-y])
                 (dispatch [:commit-object-position id])))))
  (add-pinch ham-man
             (fn [ev]
               (.stopPropagation (.-srcEvent ev)))
             (fn [ev]
               (.stopPropagation (.-srcEvent ev))))
  (fn [id]
    (let [{:keys [x-pos y-pos offset-x offset-y]} @params]
      [:div {:class            "object"
             :style (merge (if @expanded
                             obj-expanded-style
                             obj-retracted-style)
                           {:position         "fixed"
                            :left             x-pos
                            :top              y-pos
                            :transform        (str "translate3d(" offset-x "px, " offset-y "px, 0px)")
                            :transition       "min-width 0.25s, min-height 0.25s"})}
       (if @expanded
         [obj-display]
         [:p id])])))

(defn object [id init-params]
  (let [params (subscribe [:obj-params id])
        expanded (atom false)]
    [object-impl id init-params params expanded]))
