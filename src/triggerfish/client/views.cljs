(ns triggerfish.client.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch subscribe]]
            [triggerfish.shared.object-definitions :as obj]
            [triggerfish.client.sente-events :refer [chsk-send!]]
            [goog.string :as gstring]
            [clojure.set :as set]))

(declare click-delete touch-delete)
;;We assume that the device has no touch-screen until a touch-event is fired.
(defonce touch? (atom false))

;;Not using app-db state, because this needs to change frequently
(defonce moving-touches (atom {}))

(defn get-bounding-rect-obj
  [node]
  (let [rect (.getBoundingClientRect node)]
    {:bottom (+ (.-scrollTop (.-offsetParent node)) (.-bottom rect))
     :top (+ (.-scrollTop (.-offsetParent node)) (.-top rect))
     :left (+ (.-scrollLeft (.-offsetParent node)) (.-left rect))
     :right (+ (.-scrollLeft (.-offsetParent node)) (.-right rect))}))

(defn get-bounding-rect-io
  [node]
  (let [rect (.getBoundingClientRect node)]
    {:bottom (+ (.-scrollTop (.-offsetParent (.-offsetParent node))) (.-bottom rect))
     :top (+ (.-scrollTop (.-offsetParent (.-offsetParent node))) (.-top rect))
     :left (+ (.-scrollLeft (.-offsetParent (.-offsetParent node))) (.-left rect))
     :right (+ (.-scrollLeft (.-offsetParent (.-offsetParent node))) (.-right rect))}))

(defn update-inlet-or-outlet-position
  [id name this]
  (dispatch [:update-io-position id name (get-bounding-rect-io (reagent/dom-node this))]))

(defn update-object-dom-position
  [id this]
  (dispatch [:update-object-dom-position id (get-bounding-rect-obj (reagent/dom-node this))]))

(defn handle-touch-move
  [id pageX pageY]
  (let [[oldPageX oldPageY dom-element] (get @moving-touches id)
        translateX (- pageX oldPageX)
        translateY (- pageY oldPageY)
        style (.-style dom-element)]
    (set! (.-transform style) (str "translate(" translateX "px, " translateY "px)"))))

(defn commit-move-object
  [id x y]
  (dispatch [:move-object id x y]))

(defn outlet-component
  [id [name {:keys [type]}] _] ;;ignoring last arg; it is position and we just use it to force component-did-update
  (let [selected-outlet (subscribe [:selected-outlet])
        mode            (subscribe [:mode])]
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
     (fn [id [name {:keys [type]}] _]
       (let [[selected-obj sel-outlet-name sel-type] @selected-outlet]
        [:div {:class (if (and (= name sel-outlet-name) (= selected-obj id) (= sel-type type))
                        "outlet outlet-selected"
                        "outlet")
               :key (str id name)
               :on-click (when (nil? @mode)
                           (fn [e] (when (not @touch?) (dispatch [:select-outlet [id name type]]))))
               :on-touch-start (when (nil? @mode)
                                 (fn [e] (dispatch [:select-outlet [id name type]])))}
         (str name (when (= type :audio) "~"))]))})))

(defn inlet-component
  [id [name {:keys [type]}] _]
  (let [mode (subscribe [:mode])]
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
     (fn [id [name {:keys [type]}] _]
       [:div
        (merge
         {:class "inlet"
          :key (str id name)}
         (when (nil? @mode)
           {:on-click
            (fn [e]
              (when (not @touch?)
                (dispatch [:connect id name type])))
            :on-touch-start
            (fn [e]
              (dispatch [:connect id name type]))}))
        (str (when (= type :audio) "~") name)])})))

(defn object-name
  [id name minimized?]
  [:div {:class "object-name"
         :on-click (fn [e] (do
                             ;; (.stopPropagation e)
                             (when (not @touch?)
                               (dispatch [:click-obj-name id]))))
         :on-touch-start (fn [e] (do
                                 ;; (.stopPropagation e)
                                 (dispatch [:click-obj-name id ])))}
   [:div {:class "object-name-txt"} name]])

(defn object-component
  "Component for a Triggerfish Object."
  [[id obj-map]]
  (reagent/create-class
   {
    :component-did-mount
    (fn [this]
      (update-object-dom-position id this))
    :component-did-update
    (fn [this]
      (update-object-dom-position id this))
    :reagent-render
    (fn [[id obj-map]]
      (let [x-pos (:x-pos obj-map)
            y-pos (:y-pos obj-map)
            minimized (subscribe [:minimized])
            minimized? (get @minimized id)
            selected-outlet (subscribe [:selected-outlet])
            [selected-object outlet-name outlet-type] @selected-outlet
            selected? (= selected-object id)
            mode (subscribe [:mode])
            inlets (:inlets obj-map)
            outlets (:outlets obj-map)]
        [:div
         {:class (if selected?
                   "object object-selected"
                   "object")
          :style {
                  ;; :transform (str "translate(" x-pos "px, " y-pos "px)")
                  :left x-pos
                  :top  y-pos
                  }
          :on-touch-start (fn [e]
                            (.stopPropagation e)
                            (.preventDefault e)
                            (let [touch-event (.item (.-changedTouches e) 0)
                                  touch-id (.-identifier touch-event)
                                  pageX (.-pageX touch-event)
                                  pageY (.-pageY touch-event)
                                  dom-element (.-currentTarget e)]
                              (swap! moving-touches assoc touch-id [pageX pageY dom-element])))
           :on-touch-move (fn [e]
                            (.stopPropagation e)
                            (.preventDefault e)
                            (doall
                             (map
                              (fn [idx]
                                (let [touch-event (.item (.-changedTouches e) idx)
                                      touch-id (.-identifier touch-event)
                                      pageX (.-pageX touch-event)
                                      pageY (.-pageY touch-event)
                                      dom-element (.-currentTarget e)]
                                  (handle-touch-move
                                   touch-id pageX pageY)))
                              (range (.-length (.-changedTouches e))))))
          :on-touch-end (fn [e]
                          (.stopPropagation e)
                          (.preventDefault e)
                          (let [touch-event (.item (.-changedTouches e) 0)
                                touch-id (.-identifier touch-event)
                                pageX (.-pageX touch-event)
                                pageY (.-pageY touch-event)
                                [oldPageX oldPageY dom-element] (get @moving-touches touch-id)
                                style (.-style dom-element)
                                old-left (js/parseFloat (.-left style))
                                old-top (js/parseFloat (.-top style))
                                translateX (- pageX oldPageX)
                                translateY (- pageY oldPageY)
                                new-left (js/parseFloat (+ old-left translateX))
                                new-top (js/parseFloat (+ old-top translateY))]
                            (swap! moving-touches dissoc touch-id)
                            (set! (.-transform style) nil)
                            (set! (.-left style) (str new-left "px"))
                            (set! (.-top style) (str new-top "px"))
                            (commit-move-object id new-left new-top)))}
         [:div
          (condp = @mode
            :delete
            {:on-click (fn [e] (click-delete id))
             :on-touch-start (fn [e] (touch-delete id))}

            :control
            {:on-click (fn [e] (when (not @touch?)
                                 (dispatch [:select-control-object id])))
             :on-touch-start (fn [e] (dispatch [:select-control-object id]))}
            ;;default:
              nil
            )

          [object-name id (str (:name obj-map)) minimized?]

          (when (not minimized?)
            [:div {:class "io-cntr"}
             [:div {:class "io-column-cntr"}
              (map (fn [inlet]
                     ^{:key (str id inlet)}
                     [inlet-component id inlet [x-pos y-pos]]) ;;passing x-pos/y-pos forces an update
                   (sort-by first inlets))]
             [:div {:class "io-column-cntr"}
              (map (fn [outlet]
                     ^{:key (str id outlet)}
                     [outlet-component id outlet [x-pos y-pos]])
                   (sort-by first outlets))]])]]))}))

(defn cables-component
  []
    (let [connections (subscribe [:connections])
          positions   (subscribe [:positions])
          objects     (subscribe [:objects])
          patch-size  (subscribe [:patch-size])]
      (fn []
        (let [[width height] @patch-size]
          [:svg {:class "line-box" :style {:width width :height height}}
            [:g
            (doall (map (fn [conn]
                          (let [[[in-id inlet-name] [out-id outlet-name]] conn
                                pos1 (get @positions [in-id inlet-name])
                                pos2 (get @positions [out-id outlet-name])
                                obj1pos (get @positions in-id)
                                obj2pos (get @positions out-id)
                                bottom1 (:bottom pos1)
                                bottom2 (:bottom pos2)
                                top1 (:top pos1)
                                top2 (:top pos2)
                                left1 (:left pos1)
                                right2 (:right pos2)
                                center1 (+ top1 (* (- bottom1 top1) 0.5))
                                center2 (+ top2 (* (- bottom2 top2) 0.5))
                                y1 (if (> center1 0)
                                      center1
                                      (:top obj1pos))
                                y2 (if (> center2 0)
                                      center2
                                      (:top obj2pos))
                                x1 (if (> left1 0)
                                      left1
                                      (:left obj1pos))
                                x2 (if (> right2 0)
                                      right2
                                      (:right obj2pos))]
                            (when (and y1 y2 x1 x2)
                              ^{:key conn}
                              [:path {:stroke "#777"
                                      :fill "transparent"
                                      :stroke-width 2
                                      :d (str "M" x1 "," y1 " "
                                              ;;control points - how the heck does Max/MSP do this?
                                              "C" x1 "," (+ y1 0) " "
                                              x2 "," (- y2 0) " "
                                              x2 "," y2 )}])))
                        @connections))]]))))
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
  (reagent/create-class
   {
    :component-did-mount
    (fn [this]
      (let [dom-node (reagent/dom-node this)]
        (dispatch [:update-patch-size (.-scrollWidth dom-node) (.-scrollHeight dom-node)])))
    :component-did-update
    (fn [this]
      (let [dom-node (reagent/dom-node this)]
        (dispatch [:update-patch-size (.-scrollWidth dom-node) (.-scrollHeight dom-node)])))
    :reagent-render
    (let [objects (subscribe [:objects])
          selected-create-obj (subscribe [:selected-create-object])
          mode (subscribe [:mode])]
      (fn []
        [:div {:id "patch"

               ;; :on-click #(defonce fullscreen (.webkitRequestFullscreen (.-body js/document)))
               :on-click (when (= @mode :insert) (fn [e] (click-insert e @selected-create-obj)))
               :on-touch-start (when (= @mode :insert) (fn [e] (touch-insert e @selected-create-obj)))}
         (map (fn [obj]
                (with-meta [object-component obj]
                  {:key (first obj)})) @objects)
         [cables-component]]))}))

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
               :on-touch-start #(do (reset! touch? true)
                                    (dispatch [:select-create-object name]))
          :class (if (= name selected-name)
                   "create-obj-selector create-obj-selected"
                   "create-obj-selector")}
      (str name)]]))

(defn insert-toolbar
  []
   [:div {:class "synth-list"}
    (map (fn [name] (with-meta [create-object-selector name] {:key name})) (keys obj/objects))])

(defn mode-toggler
  [mode selected-mode txt]
  [:div {:class (str "mode-toggle"
                     (if (= selected-mode mode) (str " " (name mode) "-mode-active") (str " " (name mode) "-mode-inactive")))
         :on-click #(when (not @touch?) (dispatch [:set-mode mode]))
         :on-touch-start #(do (dispatch [:set-mode mode])
                              (reset! touch? true))}
   txt])

(defn toolbar
  []
  (let [toolbar-hidden? (subscribe [:toolbar-hidden])
        mode            (subscribe [:mode])]
     (condp = @mode
       :insert
       [:div {:class "toolbar"}
        [mode-toggler :insert @mode "+"]
        [insert-toolbar]
        [mode-toggler :delete @mode "-"]
        [mode-toggler :control @mode "!"]]

       ;;default
       [:div {:class "toolbar"}
        [mode-toggler :insert @mode "+"]
        [mode-toggler :delete @mode "-"]
        [mode-toggler :control @mode "!"]])))

;;a map of the nexus components we've added, so we know which one to destroy,
;;it doesn't really make sense to use the app-db for this state
(defonce nx-registry (atom {}))

(defn nexus-component
  [obj-id [ctrl-name props]]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (let [widget (.add js/nx (:nx-type props) (clj->js {:parent (reagent/dom-node this)}))]
        (swap! nx-registry #(assoc % (str obj-id ctrl-name) widget))

        ;;set params
        (doall (map
                (fn [[k v]] (aset widget (name k) v))
                (:nx-props props)))

        ;;set value
        (.set widget (clj->js {:value (:value props)}))

        (.sendsTo widget
                  (fn [data]
                    (let [value (aget data "value")]
                      (dispatch [:optimistic-set-control obj-id ctrl-name value])
                      (chsk-send! [:patch/set-control {:obj-id obj-id
                                                       :ctrl-name ctrl-name
                                                       :value value}]))))))
    :component-will-unmount
    (fn [this]
      (.destroy (get @nx-registry (str obj-id ctrl-name)))
      (swap! nx-registry #(dissoc % (str obj-id ctrl-name))))
    :reagent-render
    (fn []
      [:div
       [:h2 ctrl-name]])})
  )

(defn control-window
  []
  (let [mode (subscribe [:mode])
        objects (subscribe [:objects])
        selected-obj (subscribe [:selected-control-object])
        obj (get @objects @selected-obj)
        controls (:controls obj)]
    (when (and (= @mode :control) @selected-obj)
      [:div {:class "control-window"}
       [:div {:class "close-control-window"
              :on-click #(dispatch [:close-control-window])} "X"]
       (doall (map (fn [ctrl]
                     (let [key (str @selected-obj (first ctrl) "ctrl")]
                       ^{:key key}
                       [nexus-component @selected-obj ctrl]))
                   controls))])))

(defn app
  []
  [:div {:class "one-hundred"}
   [toolbar]
   [patch-component]
   [control-window]])
