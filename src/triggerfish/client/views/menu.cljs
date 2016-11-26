(ns triggerfish.client.views.menu
  (:require
   [reagent.core :refer [atom create-class]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-pan add-swipe]]
   [re-frame.core :refer [subscribe dispatch]])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

(defn menu-item-1 []
  [:div
   {:class "menu-item"
    :style {:background-color "#f4d"}}
   [:h1 "header yo"]
   [:p "stuff"]
   [:p "content"]])

(defn menu-item-2 []
  [:div
   {:class "menu-item"
    :style {:background-color "#ea3"}}
   [:h1 "headeryeadery yo"]
   [:p "stuff"]])

(defn menu-item-3 []
  [:div
   {:class "menu-item"
    :style {:background-color "#efc"}}
   [:h1 "header yo"]
   [:p "stuff"]])

(deftouchable menu-impl [selected-menu & menu-items]
  (add-swipe ham-man
             (fn [ev]
               (dispatch [:swipe-menu (.-direction ev) (count menu-items)])))
  (fn [selected-menu & menu-items]
    [:div
     {:class "menu"}
     [:div
      {:class "h-row"
       :style {:transition "transform 0.05s ease-in"
               :transform (str "translate(" (* -100 @selected-menu) "%, 0%)")}}
      (map-indexed (fn wrap-with-vec [idx arg]
                     (with-meta [arg] {:key (str "menu-item-" idx)}))
                   menu-items)]]))

(defn menu []
  (let [selected-menu (subscribe [:selected-menu])]
    [menu-impl selected-menu menu-item-1 menu-item-2 menu-item-3]))
