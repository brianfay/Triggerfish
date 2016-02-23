(ns triggerfish.client.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [triggerfish.shared.object-definitions :as obj]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

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

(defn move-object-mouse
  [obj-id mouse_ev]
  (dispatch [:move-object obj-id (.-clientX mouse_ev) (.-clientY mouse_ev)]))

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
          y-pos (:y-pos obj-map)]
      [:div {:class "object"
             :on-mouse-down (partial move-object-mouse id)
             :style {:left x-pos
                     :top  y-pos}}
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

(defn patch-component
  []
  (let [objects (subscribe [:objects])
        selected-obj (subscribe [:selected-create-object])]
    (fn []
      [:div {:id "patch"
             :on-click (fn [e] (when (not @touch?) (create-object @selected-obj (.-clientX e) (.-clientY e))))
             :on-touch-start (fn [e] (let [touch-events (js->clj (.-touches e))
                                           length (.-length touch-events)]
                                       (reset! touch? true)
                                       ;;touch event is fired each time we put a finger down
                                       ;;if other fingers are already down they will be in the
                                       ;;touch-events list. Just use the last one in the list
                                       (let [ev (.item touch-events (dec length))]
                                         (create-object @selected-obj
                                                        (.-pageX ev)
                                                        (.-pageY ev)))))}
       (map (fn [obj]
              (with-meta [object-component obj]
                {:key (first obj)})) @objects)
       [cables-component]
       ])))

(defn create-object-selector
  [name]
  (let [selected-obj (subscribe [:selected-create-object])
        selected-name @selected-obj]
    [:p {:on-click #(when (not @touch?)(dispatch [:select-create-object name]))
         :on-touch-start #(do (reset! touch? true) (dispatch [:select-create-object name]))
         :class (if (= name selected-name)
                  "create-obj-selected"
                  "create-obj-selector")}
    (str name)]))

(defn left-bar
  []
  [:div {:class "leftbar"}
   [:p {:style {:text-align "right"}}
    "X"]
   (map (fn [name] (with-meta [create-object-selector name] {:key name})) (keys obj/objects))])

(defn app
  []
  [:div {:class "one-hundred"}
   [left-bar]
   [patch-component]
   ])
