(ns triggerfish.client.subs
  (:require [re-frame.core :refer [reg-sub]]))

;;Objects:

(reg-sub
 :object-ids
 (fn [db _]
   (keys (:objects db))))

(reg-sub
 :obj-types
 (fn [db _]
   (keys (:obj-defs db))))

(reg-sub
 :obj-params
 (fn [db [_ id]]
   (select-keys (get-in db [:objects id]) [:x-pos :y-pos :inlets :outlets :name :offset-x :offset-y])))


(reg-sub
 :inspected-object
 (fn [db [_]]
   (let [selected-obj-id (get-in db [:menu :inspected-object])
         {:keys [controls name obj-id]}(get-in db [:objects selected-obj-id])]
     {:control-names (keys controls) :name name :obj-id obj-id})))

(reg-sub
 :selected-outlet
 (fn [db [_]]
   (:selected-outlet db)))

(reg-sub
 :control-params
 (fn [db [_ obj-id ctl-name]]
   (get-in db [:objects obj-id :controls ctl-name :params])))

(reg-sub
 :control-val
 (fn [db [_ obj-id ctl-name]]
   (get-in db [:objects obj-id :controls ctl-name :val])))

(reg-sub
 :connections
 (fn [db [_ id]]
   (:connections db)))

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
 :menu-displaying
 (fn [db _]
   (get-in db [:menu :displaying])))

(reg-sub
 :selected-add-obj
 (fn [db _]
   (get-in db [:menu :selected-add-obj])))

;;MIDI:
(reg-sub
 :recently-fiddled
 (fn [db _]
   (get-in db [:recently-fiddled])))
