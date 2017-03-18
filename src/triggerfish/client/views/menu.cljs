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

(defn main-menu
  "The menu that displays on app startup - displays a list of objects that can be added"
  []
  (let [obj-types     (subscribe [:obj-types])
        selected-add-obj (subscribe [:selected-add-obj])]
    [:div
     (map (fn [obj-name]
            ^{:key (str "object-li: " obj-name)}
            [object-li obj-name selected-add-obj])
          @obj-types)]))

(defn touch-controls
  "Interactive widgets for controlling an object"
  [obj-id control-names]
  [:div {:style {:display "flex"
                 :flex 1
                 :flex-direction "column"
                 :height "80%"}}
   (map (fn [ctl-name]
          ^{:key (str "ctl-" obj-id ctl-name)}
          [ctl/control obj-id ctl-name])
        control-names)])

(defn midi-control [[port-name status-type channel first-data-byte]]
  [:div {:class "midi-control"}
   ;; [:p port-name]
   [:p (interpose " " [status-type channel first-data-byte])]])

(defn midi-controls
  [obj-id control-names]
  (let [fiddled (subscribe [:recently-fiddled])]
    [:div
     [:div control-names]
     [:div (map (fn [m]
                  ^{:key (str "midi-control-" m)}
                  [midi-control m])
                (filter some? @fiddled))]]))

(defn inspector
  "A display for interactions with a specific object - like setting controls, subscribing MIDI listeners, deleting the object"
  []
  (let [obj (subscribe [:inspected-object])
        {:keys [name obj-id control-names]} @obj]
    [:div
     {:style {:display "flex"
              :height "100%"
              :flex-direction "column"}}
     [:div [:h1 {:style
                 {:text-align "center"}}
            [:div name]]
      [:hr]]
    [touch-controls obj-id control-names]
     ;; [midi-controls obj-id control-names]
     [:div {:class "delete-button"
            :on-click (fn [e] (dispatch [:delete-object obj-id]))}
      "delete"]]))

(defn menu
  "A menu on the right-hand side of the screen that handles interactions like selecting object types or interacting with objects"
  []
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
