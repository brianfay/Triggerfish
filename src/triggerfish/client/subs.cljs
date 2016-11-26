(ns triggerfish.client.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :objects
 (fn [db _]
   (keys (:objects db))))

(reg-sub
 :obj-params
 (fn [db [_ id]]
   (get-in db [:objects id])))

(reg-sub
 :camera-position
 (fn [db _]
   (let [{:keys [x-pos y-pos offset-x offset-y]} (:pan db)
         x-pos                                   (+ x-pos offset-x)
         y-pos                                   (+ y-pos offset-y)]
     [x-pos y-pos])))

(reg-sub
 :zoom
 (fn [db _]
   (* (get-in db [:zoom :scale])
      (get-in db [:zoom :current-zoom]))))

(reg-sub
 :selected-menu
 (fn [db _]
   (get-in db [:menu :selected])))
