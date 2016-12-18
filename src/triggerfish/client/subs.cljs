(ns triggerfish.client.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

;;Objects:

(reg-sub
 :objects
 (fn [db _]
   (:objects db)))

(reg-sub
 :obj-params
 (fn [db [_ id]]
   (get-in db [:objects id])))

(reg-sub
 :selected-outlet
 (fn [db [_ id]]
   (:selected-outlet db)))

(reg-sub
 :connections
 (fn [db [_ id]]
   (get-in db [:objects :connections])))

(reg-sub
 :object-position
 (fn [db [_ id]]
   (let [{:keys [x-pos y-pos offset-x offset-y]} (get-in db [:objects id])]
     [(+ x-pos offset-x) (+ y-pos offset-y)])))

;;Camera:

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

;;Menu:

(reg-sub
 :menu-visibility
 (fn [db _]
   (get-in db [:menu :visibility])))

(reg-sub
 :menu-position
 (fn [db _]
   (select-keys (get-in db [:menu :position]) [:x :y])))

(reg-sub
 :selected-menu
 (fn [db _]
   (get-in db [:menu :selected])))

(reg-sub
 :selected-obj-to-insert
 (fn [db _]
   (get-in db [:menu :selected-obj])))
