(ns triggerfish.client.views.menu
  (:require
   [reagent.core :refer [atom create-class]]
   [triggerfish.client.utils.hammer :refer [hammer-manager add-pan add-swipe]]
   [re-frame.core :refer [subscribe dispatch]]
   [triggerfish.shared.object-definitions :as obj])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

(defn object-li [obj-name selected-obj]
  (let [selected? (= @selected-obj obj-name)]
    [:p {:class (when selected? "selected-obj")
         :on-click (fn [e]
                     (.stopPropagation e)
                     (dispatch [:select-obj-to-insert obj-name]))}
     obj-name]))

(defn object-selector []
  (let [selected-obj (subscribe [:selected-obj-to-insert])]
    [:div
     {:class "menu-item"}
     [:h1 "Objects"]
     (map (fn [obj-name]
            ^{:key (str "object-li: " obj-name)}
            [object-li obj-name selected-obj])
          (keys obj/objects))]))

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

(deftouchable menu-impl [selected-menu menu-visible? menu-position & menu-items]
  (add-swipe ham-man
             (fn [ev]
               (dispatch [:swipe-menu (.-direction ev) (count menu-items)])))
  (fn [selected-menu menu-visible? menu-position & menu-items]
    (let [pos @menu-position
          x (:x pos)
          y (:y pos)]
      [:div
       {:class "menu"
        :style {:visibility (when-not @menu-visible? "hidden")
                :left (str x "px")
                :top  (str y "px")}}
       [:div
        {:class "h-row"
         :style {:transition "transform 0.05s ease-in"
                 :transform (str "translate(" (* -100 @selected-menu) "%, 0%)")}}
        (map-indexed (fn wrap-with-vec [idx arg]
                       (with-meta [arg] {:key (str "menu-item-" idx)}))
                     menu-items)]])))

(defn menu []
  (let [selected-menu (subscribe [:selected-menu])
        menu-visible? (subscribe [:menu-visibility])
        menu-position (subscribe [:menu-position])]
    [menu-impl selected-menu menu-visible? menu-position object-selector #_menu-item-2 #_menu-item-3]))
