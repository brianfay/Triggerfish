(ns triggerfish.client.views.menu
  (:require
   [re-frame.core :refer [subscribe dispatch]]))

(defn object-li [obj-name selected-obj]
  (let [selected? (= @selected-obj obj-name)]
    [:p {:class (when selected? "selected-obj")
         :on-click (fn [e]
                     (.stopPropagation e)
                     (dispatch [:select-obj-to-insert obj-name]))}
     obj-name]))

(defn object-selector []
  (let [obj-defs     (subscribe [:obj-defs])
        selected-obj (subscribe [:selected-obj-to-insert])]
    [:div
     (map (fn [obj-name]
            ^{:key (str "object-li: " obj-name)}
            [object-li obj-name selected-obj])
          (keys @obj-defs))]))

(defn action-toggle [action-name]
  (let [selected-action (subscribe [:selected-action])
        selected? (= @selected-action action-name)]
    [:div {:class (str "action-toggle" " "
                       (str action-name "-toggle-" (if selected? "active" "inactive")))
           :on-click (fn [ev]
                       (.stopPropagation ev)
                       (dispatch [:select-action action-name]))}
     action-name]))

(defn action-selector []
  [:div  {:class "action-selector"}
   (action-toggle "add")
   (action-toggle "delete")])

(defn menu []
  (let [menu-visible? (subscribe [:menu-visibility])
        selected-action (subscribe [:selected-action])]
  [:div
   {:class "menu"
    :on-click (fn [ev] (.stopPropagation ev))
    :style {:left (if @menu-visible? "0%" "-100%")}}
   [action-selector]
   (condp = @selected-action
       "add" [object-selector]
       nil)]))
