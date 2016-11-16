(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [reg-event-db]]))

(def initial-state
  {:objects {1 {:x-pos 100 :y-pos 100}
             2 {:x-pos 200 :y-pos 200}}
   :pan     {:x-pos 0 :y-pos 0 :offset-x 0 :offset-y 0}
   :zoom    {:current-zoom 1 :scale 1}})

(reg-event-db
 :initialize
 (fn [db _]
   (merge db initial-state)))

(reg-event-db
 :offset-object
 (fn [db [_ id delta-x delta-y]]
   (let [zoom (get-in db [:zoom :current-zoom])
         scale-offset (/ 1 zoom)
         offset-x (* delta-x scale-offset)
         offset-y (* delta-y scale-offset)]
     (-> db
         (assoc-in [:objects id :offset-x] offset-x)
         (assoc-in [:objects id :offset-y] offset-y)))))

(reg-event-db
 :commit-object-position
 (fn [db [_ id]]
   (let [offset-x (get-in db [:objects id :offset-x])
         offset-y (get-in db [:objects id :offset-y])
         x-pos (get-in db [:objects id :x-pos])
         y-pos (get-in db [:objects id :y-pos])]
     (-> db
        (assoc-in [:objects id :offset-x] 0)
        (assoc-in [:objects id :offset-y] 0)
        (assoc-in [:objects id :x-pos] (+ x-pos offset-x))
        (assoc-in [:objects id :y-pos] (+ y-pos offset-y))))))

(reg-event-db
 :commit-camera-pan
 (fn [db _]
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
 (fn [db [_ delta-x delta-y]]
   (-> db
       (assoc-in [:pan :offset-x] (+ (:x-pos db) delta-x))
       (assoc-in [:pan :offset-y] (+ (:y-pos db) delta-y)))))

(reg-event-db
 :commit-camera-zoom
 (fn [db _]
   (let [current-zoom (get-in db [:zoom :current-zoom])
         scale        (get-in db [:zoom :scale])]
     (-> db
         (assoc-in [:zoom :current-zoom] (* current-zoom scale))
         (assoc-in [:zoom :scale] 1)))))

(reg-event-db
 :zoom-camera
 (fn [db [_ scale]]
   (assoc-in db [:zoom :scale] scale)))
