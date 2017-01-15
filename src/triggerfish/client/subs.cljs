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
 :inlet-position
 (fn [db [_ obj-id inlet-name]]
   (let [{:keys [x-pos y-pos offset-x offset-y]} (get-in db [:objects obj-id])
         offset-top (get-in db [:inlet-offsets obj-id inlet-name])]
     [(+ x-pos offset-x) (+ y-pos offset-y offset-top)])))

(reg-sub
 :outlet-position
 (fn [db [_ obj-id outlet-name]]
   (let [{:keys [x-pos y-pos offset-x offset-y]} (get-in db [:objects obj-id])
         offset-top (get-in db [:outlet-offsets obj-id outlet-name])
         obj-width  (get-in db [:object-widths obj-id])]
     [(+ x-pos offset-x obj-width) (+ y-pos offset-y offset-top)])))

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
 :selected-action
 (fn [db _]
   (get-in db [:menu :selected-action])))

(reg-sub
 :selected-obj-to-insert
 (fn [db _]
   (get-in db [:menu :selected-obj])))
