(ns triggerfish.client.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [triggerfish.shared.object-definitions :as obj]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

(declare click-delete touch-delete)
;;We assume that the device has no touch-screen until a touch-event is fired.
(defonce touch? (atom false))

(defn get-bounding-rect
  [node]
  (let [rect (.getBoundingClientRect node)]
    {:bottom (+ (.-scrollY js/window) (.-bottom rect))
     :top (+ (.-scrollY js/window) (.-top rect))
     :left (.-left rect)
     :right (.-right rect)}))

(defn update-inlet-or-outlet-position
  [id name this]
  (dispatch [:update-position id name (get-bounding-rect (reagent/dom-node this))]))

(defn outlet-component
  [id name _] ;;ignoring last arg; it is position and we just use it to force component-did-update
  (reagent/create-class
   {
    :component-did-mount
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :component-did-update
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :component-will-unmount
    (fn [this]
      (dispatch [:dissoc-position id name]))
    :reagent-render
    (fn [id name _]
      [:div {:class "outlet" :key (str id name)}
       name])}))

(defn inlet-component
  [id name _]
  (reagent/create-class
   {
    :component-did-mount
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :component-did-update
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :component-will-unmount
    (fn [this]
      (dispatch [:dissoc-position id name]))
    :reagent-render
    (fn [id name _]
      [:div {:class "inlet" :key (str id name)}
       ;; [connector-icon id name _]
       name])}))

(defn object-component
  "Component for a Triggerfish Object."
  [[id obj-map]]
  (fn [[id obj-map]]
    (let [x-pos (:x-pos obj-map)
          y-pos (:y-pos obj-map)
          mode (subscribe [:mode])]
      [:div
       (merge
        {:class "object"
         :style {:left x-pos
                 :top  y-pos}}
        (condp = @mode
          :delete
            {:on-click (fn [e] (click-delete id))
            :on-touch-start (fn [e] (touch-delete id))}
          nil))
        [:div (str (:name obj-map))]
        (map (fn [name]
              ^{:key (str id name)}
              [inlet-component id name [x-pos y-pos]]) ;;passing x-pos/y-pos forces an update
            (keys (:inlets obj-map)))
        (map (fn [name]
              ^{:key (str id name)}
              [outlet-component id name [x-pos y-pos]])
            (keys (:outlets obj-map)))])))

(defn cables-canvas
  []
  (let [connections (subscribe [:connections])
        positions   (subscribe [:positions])
        ]
    (reagent/create-class
     {
     :component-did-mount
     (fn [this]
       (doall (map
               (fn [conn]
                 (let [
                       [[in-id inlet-name] [out-id outlet-name]] conn
                       pos1 (get @positions [in-id inlet-name])
                       pos2 (get @positions [out-id outlet-name])
                       x-left (:left pos1)
                       x-right (:right pos1)
                       x-bottom (:bottom pos1)
                       x-top (:top pos1)
                       y-left (:left pos2)
                       y-right (:right pos2)
                       y-bottom (:bottom pos2)
                       y-top (:top pos2)
                       canvas (reagent/dom-node this)
                       ctx (.getContext canvas "2d")]
                   (println "fill it " x-left x-top (- x-right x-left) (- x-bottom x-top))
                   (.fillRect ctx x-left x-top (- x-right x-left) (- x-bottom x-top))
                   ;; (.fillRect ctx 260 542 64 15)

                   (.fillRect ctx 60 42 64 15)

                   ;; (.clearRect ctx 45 45 60 60)
                   )
                   ;; (.strokeRect 50 50 50 50))
                 )
              @connections)))
     :reagent-render
     (fn []
         [:canvas {:class "line-box" :id "canvas"}])})))

(defn cables-component
  []
  (let [connections (subscribe [:connections])
        positions   (subscribe [:positions])]
    (fn []
      [:svg {:class "line-box"}
       [:g
        (doall (map (fn [conn]
                      (let [[[in-id inlet-name] [out-id outlet-name]] conn
                            pos1 (get @positions [in-id inlet-name])
                            pos2 (get @positions [out-id outlet-name])
                            x1 (or (:left pos1) 0)
                            y1 (or (:top pos1) 0)
                            x2 (or (:right pos2) 0)
                            y2 (or (:top pos2) 0)]
                        ^{:key conn}
                        [:path {:stroke "white"
                                :fill "transparent"
                                :stroke-width 2
                                :d (str "M" x1 "," y1 " "
                                        ;;control points - how the heck does Max/MSP do this?
                                        "C" x1 "," (+ y1 50) " "
                                        x2 "," (- y2 50) " "

                                        x2 "," y2 )}]))
                    @connections))]])))
(defn create-object
  [obj-name x-pos y-pos]
  (if (nil? obj-name)
    nil
    (do
      (dispatch [:optimistic-create obj-name x-pos y-pos])
      (chsk-send! [:patch/create-object
                  {:name obj-name
                   :x-pos x-pos
                   :y-pos y-pos}]))))

(defn delete-object
  [obj-id]
  (do
    (dispatch [:optimistic-delete obj-id])
    (chsk-send! [:patch/delete-object {:obj-id obj-id}])))

(defn click-insert
  [e selected-obj]
  (when (not @touch?) (create-object selected-obj (.-clientX e) (.-clientY e))))

(defn touch-insert
  [e selected-obj]
  (let [touch-events (js->clj (.-touches e))
        length (.-length touch-events)]
    (reset! touch? true)
    ;;touch event is fired each time we put a finger down
    ;;if other fingers are already down they will be in the
    ;;touch-events list. Just use the last one in the list
    (let [ev (.item touch-events (dec length))]
      (create-object selected-obj
                     (+ (.-scrollLeft (.getElementById js/document "patch")) (.-pageX ev))
                     (+ (.-scrollTop (.getElementById js/document "patch")) (.-pageY ev))))))

(defn click-delete
  [obj-id]
  (when (not @touch?) (delete-object obj-id)))

(defn touch-delete
  [obj-id]
  (do (reset! touch? true)
      (delete-object obj-id)))

(defn patch-component
  []
  (let [objects (subscribe [:objects])
        selected-obj (subscribe [:selected-create-object])
        mode (subscribe [:mode])]
    (fn []
      [:div (merge {:id "patch"}
                   (condp = @mode
                     :insert {
                              :on-click (fn [e] (click-insert e @selected-obj))
                              :on-touch-start (fn [e] (touch-insert e @selected-obj))}
                     nil))
       (map (fn [obj]
              (with-meta [object-component obj]
                {:key (first obj)})) @objects)
       [cables-component]])))

(defn mode-selector
  []
  (let [mode (subscribe [:mode])]
       [:div {:class "mode-selector"}
        [:div {:class (if (= @mode :insert) "mode-toggle insert-mode-active"
                          "mode-toggle insert-mode-inactive")
               :on-click #(dispatch [:set-mode :insert])
               :on-touch-start #(do (reset! touch? true) (dispatch [:set-mode :insert]))} "insert"]
        [:div {:class (if (= @mode :delete) "mode-toggle delete-mode-active"
                          "mode-toggle delete-mode-inactive")
               :on-click #(dispatch [:set-mode :delete])
               :on-touch-start #(do (reset! touch? true) (dispatch [:set-mode :delete]))} "delete"]
        [:div {:class (if (= @mode :connect) "mode-toggle connect-mode-active"
                          "mode-toggle connect-mode-inactive")
               :on-click #(dispatch [:set-mode :connect])
               :on-touch-start #(do (reset! touch? true) (dispatch [:set-mode :connect]))} "connect"]
        [:div {:class (if (= @mode :move) "mode-toggle move-mode-active"
                          "mode-toggle move-mode-inactive")
               :on-click #(dispatch [:set-mode :move])
               :on-touch-start #(do (reset! touch? true) (dispatch [:set-mode :move]))} "move"]]))

(defn create-object-selector
  [name]
  (let [selected-obj (subscribe [:selected-create-object])
        selected-name @selected-obj]
    [:div [:p {:on-click #(when (not @touch?) (dispatch [:select-create-object name]))
          :on-touch-start #(do (reset! touch? true) (dispatch [:select-create-object name]))
          :class (if (= name selected-name)
                   "create-obj-selector create-obj-selected"
                   "create-obj-selector")}
      (str name)]]))

(defn sidebar-show-toggle
  []
  (let [open (subscribe [:sidebar-open])]
    (if @open
      [:p {:class "min-max-icon min-max-open-slide"
           :on-click #(when (not @touch?) (dispatch [:close-sidebar]))
           :on-touch-start #(do (reset! touch? true) (dispatch [:close-sidebar]))} [:span {:class "min-max-open-flip"} "< "]]
      [:p {:class "min-max-icon min-max-close-slide"
           :on-click #(when (not @touch?) (dispatch [:open-sidebar]))
           :on-touch-start #(do (reset! touch? true) (dispatch [:open-sidebar]))} [:span {:class "min-max-close-flip"} "< "]])))

(defn sidebar
  []
  (let [open (subscribe [:sidebar-open])
        mode (subscribe [:mode])]
    [:div {:class (if @open "sidebar sidebar-opened"
                      "sidebar sidebar-closed")}
     [sidebar-show-toggle]
     [:div {:class "title"} [:h1 "TRIGGERFISH"]]
     [mode-selector]
     [:div {:class "sidebar-content"}
      (condp = @mode
          :insert (map (fn [name] (with-meta [create-object-selector name] {:key name})) (keys obj/objects))
          :delete nil
          :connect nil
          :move nil
          nil)]]))

(defn app
  []
  [:div {:class "one-hundred"}
   [sidebar]
   [patch-component]])
