(ns triggerfish.client.views.objects
  (:require
   [reagent.core :refer [create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [add-pan add-pinch]]
   [re-frame.core :refer [subscribe dispatch]])
  (:require-macros
   [triggerfish.client.utils.macros :refer [deftouchable]]))

(defn alphabetical-comparator [[k1 v1] [k2 v2]] (< k1 k2))

(defn inlet [obj-id [inlet-name inlet-params]]
  (create-class
   {:component-did-mount
    (fn [this] ;;store inlet offset relative to parent div, so cables can be drawn correctly
      (let [el (dom-node this)
            offset-top (.-offsetTop el)
            offset-height (.-offsetHeight el)]
        (dispatch [:register-inlet-offset obj-id inlet-name (+ offset-top (/ offset-height 2))])))
    :reagent-render
    (fn [obj-id [inlet-name inlet-params]]
      (let [{:keys [type]} inlet-params]
        [:div {:class "inlet"
               :on-click (fn [e] (dispatch [:click-inlet obj-id inlet-name type]))}
         (str (when (= type :audio) "~") (name inlet-name))]))}))

(defn outlet [obj-id [outlet-name outlet-params]]
  (create-class
   {:component-did-mount
    (fn [this]
      (let [el (dom-node this)
            offset-top (.-offsetTop el)
            offset-height (.-offsetHeight el)]
        (dispatch [:register-outlet-offset obj-id outlet-name (+ offset-top (/ offset-height 2))])))
    :reagent-render
    (fn [obj-id [outlet-name outlet-params]]
      (let [{:keys [type]}  outlet-params
            selected-outlet (subscribe [:selected-outlet])
            selected? (= [obj-id outlet-name type] @selected-outlet)]
        [:div {:class (if selected? "outlet selected-outlet" "outlet")
               :on-click (fn [e] (dispatch [:click-outlet obj-id outlet-name type]))}
         (str (name outlet-name) (when (= type :audio) "~"))]))}))

(defn obj-display [id {:keys [outlets inlets]} as params]
  [:div {:class "object-display"}
   [:div {:class "io-container"}
    (map (fn [in]
           ^{:key (str id "inlet:" (first in))}
           [inlet id in])
         (sort alphabetical-comparator inlets))]
   [:div {:class "io-container"}
    (map (fn [out]
           ^{:key (str id "outlet:" (first out))}
           [outlet id out])
         (sort alphabetical-comparator outlets))]])

(deftouchable object-impl [id params]
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
  (fn [id params]
    (let [params @params
          selected-action (subscribe [:selected-action])
          {:keys [x-pos y-pos offset-x offset-y name]} params]
      [:div {:class "object"
             :style {:position         "fixed"
                     :left             x-pos
                     :top              y-pos
                     :transform        (str "translate3d(" offset-x "px, " offset-y "px, 0px)")
                     :transition       "min-width 0.25s, min-height 0.25s"}
             :on-click (fn [e]
                         (.stopPropagation e)
                         (when (= @selected-action "delete") (dispatch [:object-clicked id])))}
       [:p name]
       [obj-display id params]])))

(defn object [id]
  (create-class
   {:component-did-mount
    (fn [this] ;;need to track object width to render cables properly
      (dispatch [:register-object-width id (.-offsetWidth (dom-node this))]))
    :reagent-render
    (fn [id]
      (let [params (subscribe [:obj-params id])]
        [object-impl id params]))}))
