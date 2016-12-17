(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx debug trim-v]]
            [triggerfish.client.sente-events :refer [chsk-send!]]
            [triggerfish.shared.object-definitions :as obj-def]))

(def standard-interceptors [trim-v #_debug])

(def initial-state
  {:objects {}
   :pan     {:x-pos 0 :y-pos 0 :offset-x 0 :offset-y 0}
   :zoom    {:current-zoom 1 :scale 1}
   :menu    {:selected 0
             :visibility false
             :position {:x 0 :y 0}}})

(reg-event-db
 :initialize
 standard-interceptors
 (fn [db _]
   (merge db initial-state)))

(reg-event-db
 :patch-recv
 standard-interceptors
 (fn [db [patch]]
   (assoc db :objects patch)))

;;Objects:

(reg-fx
 :move-object
 (fn [[obj-id x y]]
   (chsk-send!
    [:patch/move-object
     {:obj-id obj-id
      :x-pos x
      :y-pos y}])))

(reg-event-db
 :deselect-outlet
 standard-interceptors
 (fn [db]
   (assoc db :selected-outlet nil)))

(reg-event-db
 :click-outlet
 standard-interceptors
 (fn [db [obj-id outlet-name type]]
   (if-not (= [obj-id outlet-name type] (:selected-outlet db))
     (assoc db :selected-outlet [obj-id outlet-name type])
     (assoc db :selected-outlet nil))))

(reg-event-fx
 :click-inlet
 standard-interceptors
 (fn [{:keys [db]} [obj-id inlet-name type]]
   (let [selected-outlet (:selected-outlet db)
         [out-obj-id _ outlet-type] selected-outlet]
     (if (and selected-outlet (not= out-obj-id obj-id) (= type outlet-type))
       {:dispatch [:deselect-outlet]}
       {}))))

(reg-event-db
 :offset-object
 standard-interceptors
 (fn [db [id delta-x delta-y]]
   (let [zoom (get-in db [:zoom :current-zoom])
         scale-offset (/ 1 zoom)
         offset-x (* delta-x scale-offset)
         offset-y (* delta-y scale-offset)]
     (-> db
         (assoc-in [:objects id :offset-x] offset-x)
         (assoc-in [:objects id :offset-y] offset-y)))))

(reg-event-fx
 :commit-object-position
 standard-interceptors
 (fn [{:keys [db]} [id]]
   (let [offset-x (get-in db [:objects id :offset-x])
         offset-y (get-in db [:objects id :offset-y])
         x-pos (get-in db [:objects id :x-pos])
         y-pos (get-in db [:objects id :y-pos])
         x (+ x-pos offset-x)
         y (+ y-pos offset-y)]
     {:db (-> db
              (assoc-in [:objects id :offset-x] 0)
              (assoc-in [:objects id :offset-y] 0)
              (assoc-in [:objects id :x-pos] x)
              (assoc-in [:objects id :y-pos] y))
      :move-object [id x y]})))

;;Camera:

(reg-event-db
 :commit-camera-pan
 standard-interceptors
 (fn [db]
   (let [x-pos (get-in db [:pan :x-pos])
         y-pos (get-in db [:pan :y-pos])
         offset-x (get-in db [:pan :offset-x])
         offset-y (get-in db [:pan :offset-y])]
     (-> db
         (assoc-in [:pan :x-pos] (+ x-pos offset-x))
         (assoc-in [:pan :y-pos] (+ y-pos offset-y))
         (assoc-in [:pan :offset-x] 0)
         (assoc-in [:pan :offset-y] 0)))))

(reg-event-db
 :pan-camera
 standard-interceptors
 (fn [db [delta-x delta-y]]
   (-> db
       (assoc-in [:pan :offset-x] (+ (:x-pos db) delta-x))
       (assoc-in [:pan :offset-y] (+ (:y-pos db) delta-y)))))

(reg-event-db
 :commit-camera-zoom
 standard-interceptors
 (fn [db]
   (let [current-zoom (get-in db [:zoom :current-zoom])
         scale        (get-in db [:zoom :scale])]
     (-> db
         (assoc-in [:zoom :current-zoom] (* current-zoom scale))
         (assoc-in [:zoom :scale] 1)))))

(reg-event-db
 :zoom-camera
 standard-interceptors
 (fn [db [scale]]
   (assoc-in db [:zoom :scale] scale)))

;;Menu:

(reg-fx
 :add-object
 (fn [[obj-name x y]]
   (chsk-send! [:patch/create-object
                {:name obj-name
                 :x-pos x
                 :y-pos y}])))

(reg-event-db      ;;a mock object for optimistic ui updates
 :add-ghost-object
 standard-interceptors
 (fn [db [selected-obj x y]]
   (println (:objects db))
   (let [obj-def (get obj-def/objects selected-obj)]
     (-> db
         (assoc-in [:menu :selected-obj] nil)
         (assoc-in [:objects (str "ghost-" (rand-int 100000))]
                   (merge
                    obj-def
                    {:name selected-obj :x-pos x :y-pos y}))))))

(defn translate-and-scale-points [db x y]
  (let [zoom (* (get-in db [:zoom :scale])
                (get-in db [:zoom :current-zoom]))
        {:keys [x-pos y-pos offset-x offset-y]} (:pan db)
        x-translate (+ x-pos offset-x)
        y-translate (+ y-pos offset-y)
        x (-> x
              (- x-translate)
              (/ zoom))
        y (-> y
              (- y-translate)
              (/ zoom))]
    [x y]))

(reg-event-fx
 :app-container-clicked
 standard-interceptors
 (fn [{:keys [db]} [x y]]
   (let [visible?            (get-in db [:menu :visibility])
         [scaled-x scaled-y] (translate-and-scale-points db x y)
         selected-obj        (get-in db [:menu :selected-obj])]
     (merge
      {:db (-> db
               (assoc-in [:menu :visibility] (not visible?))
               (assoc-in [:menu :position :x] x)
               (assoc-in [:menu :position :y] y))}
      (if (and visible? selected-obj)
        {:add-object [selected-obj scaled-x scaled-y]
         :dispatch   [:add-ghost-object     selected-obj scaled-x scaled-y]}
        {})))))

(reg-event-db
 :swipe-menu
 standard-interceptors
 (fn [db [direction total-menu-items]]
   (let [selected (get-in db [:menu :selected])
         new-val (condp = direction
                   (.-DIRECTION_LEFT js/Hammer)
                   (inc selected)
                   (.-DIRECTION_RIGHT js/Hammer)
                   (dec selected)
                   selected)]
     (if (and (>= new-val 0)
              (< new-val total-menu-items))
       (assoc-in db [:menu :selected] new-val)
       db))))

(reg-event-db
 :select-obj-to-insert
 standard-interceptors
 (fn [db [obj-name]]
   (if (not= obj-name (get-in db [:menu :selected-obj]))
     (assoc-in db [:menu :selected-obj] obj-name)
     (assoc-in db [:menu :selected-obj] nil))))
