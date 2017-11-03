(ns triggerfish.client.views.objects
  (:require
   [reagent.core :refer [create-class dom-node]]
   [triggerfish.client.utils.hammer :refer [add-pan add-pinch]]
   [re-frame.core :refer [dispatch]]
   [triggerfish.client.utils.helpers :refer [listen]])
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
        [:div.inlet {:on-click (fn [e] (dispatch [:click-inlet obj-id inlet-name type]))}
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
            selected? (= [obj-id outlet-name type] (listen [:selected-outlet]))]
        [:div {:class (if selected? "outlet selected-outlet" "outlet")
               :on-click (fn [e] (dispatch [:click-outlet obj-id outlet-name type]))}
         (str (name outlet-name) (when (= type :audio) "~"))]))}))

(defn obj-header [obj-id name]
  [:div.object-header {:on-click (fn [e] (dispatch [:object-header-clicked obj-id]))}
   name])

(defn obj-display [obj-id {:keys [outlets inlets]} as params]
  [:div.object-display
   (when (not-empty inlets)
     [:div.io-container
      (map (fn [in]
             ^{:key (str obj-id "inlet:" (first in))}
             [inlet obj-id in])
           (sort alphabetical-comparator inlets))])
   (when (not-empty outlets)
     [:div.io-container
      (map (fn [out]
             ^{:key (str obj-id "outlet:" (first out))}
             [outlet obj-id out])
           (sort alphabetical-comparator outlets))])])

(deftouchable object-impl [obj-id]
  (add-pan ham-man
           (fn [ev]
             (.stopPropagation (.-srcEvent ev)) ;; without this, you can pan the camera while moving an object by using two fingers.
             (let [delta-x (.-deltaX ev)
                   delta-y (.-deltaY ev)
                   type    (.-type ev)]
               (when (= "panstart" type)
                 (dispatch [:start-moving-object]))
               (if (or (= "panend" type) (= "pancancel" type))
                 (do
                   (dispatch [:stop-moving-object])
                   (dispatch [:commit-object-position obj-id]))
                 (do
                   (dispatch [:offset-object obj-id delta-x delta-y]))))))
  (add-pinch ham-man
             (fn [ev]
               (.stopPropagation (.-srcEvent ev)))
             (fn [ev]
               (.stopPropagation (.-srcEvent ev))))
  (fn [obj-id]
    (let [params (listen [:obj-params obj-id])
          {:keys [x-pos y-pos offset-x offset-y name]} params]
      [:div.object {:style {:position  "fixed"
                            :left      x-pos
                            :top       y-pos
                            :transform (str "translate3d(" offset-x "px, " offset-y "px, 0px)")}
             :on-click (fn [e]
                         (.stopPropagation e))}
       [obj-header obj-id name]
       [obj-display obj-id params]])))

(defn object [obj-id]
  (create-class
   {:component-did-mount
    (fn [this] ;;need to track object width to render cables properly
      (dispatch [:register-object-width obj-id (.-offsetWidth (dom-node this))]))
    :reagent-render
    (fn [obj-id]
      [object-impl obj-id])}))
