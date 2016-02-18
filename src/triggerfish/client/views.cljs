(ns triggerfish.client.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]))

(defn get-bounding-rect
  [node]
  (let [rect (.getBoundingClientRect node)]
    {:bottom (+ (.-bottom rect) (.-scrollY js/window))
     :top (+ (.-top rect) (.-scrollY js/window))
     :left (+ (.-left rect) (.-scrollX js/window))
     :right (+ (.-right rect) (.-scrollX js/window))}))

(defn update-inlet-or-outlet-position
  [id name this]
  (dispatch [:update-position id name (get-bounding-rect (reagent/dom-node this))]))

(defn outlet-component
  [id name _]
  (reagent/create-class
   {
    :component-did-mount
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :component-did-update
    (fn [this]
      (update-inlet-or-outlet-position id name this))
    :reagent-render
    (fn [id name]
      [:div {:class "outlet" :key (str id name) :ref name}
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
    :reagent-render
    (fn [id name]
      [:div {:class "inlet" :key (str id name) :ref name}
       name])}))

(defn object-component
  "Component for a Triggerfish Object."
  [[id obj-map]]
  (fn [[id obj-map]]
    (let [x-pos (:x-pos obj-map)
          y-pos (:y-pos obj-map)]
      [:div {:class "object"
              :style {:left x-pos
                      :top  y-pos}}
        [:div (str (:name obj-map))]
        (map (fn [name]
              ^{:key (str id name)}
              [inlet-component id name [x-pos y-pos]])
            (keys (:inlets obj-map)))
        (map (fn [name]
              ^{:key (str id name)}
              [outlet-component id name [x-pos y-pos]])
            (keys (:outlets obj-map)))])))

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
                            x1 (:left pos1)
                            y1 (:top pos1)
                            x2 (:right pos2)
                            y2 (:top pos2)]
                        ^{:key conn}
                        [:line {:stroke "white"
                                :stroke-width 10
                                :x1 x1 :y1 y1
                                :x2 x2 :y2 y2}])) @connections))]])))

(defn patch-component
  []
  (let [objects (subscribe [:objects])]
    (fn []
      [:div {:id "patch"}
       (map (fn [obj]
              (with-meta [object-component obj]
                {:key (first obj)})) @objects)
       [cables-component]])))

(defn app
  []
  [:div
   [patch-component]])
