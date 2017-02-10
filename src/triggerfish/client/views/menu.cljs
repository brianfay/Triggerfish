(ns triggerfish.client.views.menu
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [triggerfish.client.views.controls :as ctl]))

(defn object-li [obj-name selected-add-obj]
  (let [selected? (= @selected-add-obj obj-name)]
    [:p {:class (when selected? "selected-add-obj")
         :on-click (fn [e]
                     (.stopPropagation e)
                     (dispatch [:select-add-obj obj-name]))}
     obj-name]))

(defn main-menu []
  (let [obj-types     (subscribe [:obj-types])
        selected-add-obj (subscribe [:selected-add-obj])]
    [:div
     (map (fn [obj-name]
            ^{:key (str "object-li: " obj-name)}
            [object-li obj-name selected-add-obj])
          @obj-types)]))

(defn inspector []
  (let [obj (subscribe [:inspected-object])
        {:keys [name obj-id control-names]} @obj]
    [:div [:h1 name]
     [:div {:class "delete-button"
            :on-click (fn [e] (dispatch [:delete-object obj-id]))}
                        "delete"]
     (map (fn [ctl-name]
            ^{:key (str "ctl-" obj-id ctl-name)}
            [ctl/control obj-id ctl-name])
          control-names)]))

(defn menu []
  (let [menu-visible? (subscribe [:menu-visibility])
        displaying    (subscribe [:menu-displaying])]
    [:div
     {:class "menu"
      :on-click (fn [ev] (.stopPropagation ev))
      :style {:transform (if @menu-visible? "translateX(-100%)" nil)}}
     (condp = @displaying
       :main-menu    [main-menu]
       :inspector    [inspector]
       nil)]))
