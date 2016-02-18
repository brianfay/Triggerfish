(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [register-handler]]))

(def initial-state
  {:objects {}
   :connections {}
   :positions {}
   })

(register-handler
 :initialize
 (fn
   [db _]
   (merge db initial-state)))

(register-handler
 :patch-recv
 (fn [db [ev-id patch]]
   (assoc db :objects (dissoc patch :connections) :connections (:connections patch))))

;;Updates the app-db with the DOM position of an inlet or outlet
(register-handler
 :update-position
 (fn [db [ev-id obj-id inlet-or-outlet-name pos]]
   (assoc-in db [:positions [obj-id inlet-or-outlet-name]] pos)))

(register-handler
 :move-object
 (fn [db [ev-id obj-id x y]]
   (-> db
    (assoc-in [:objects obj-id :x-pos] x)
    (assoc-in [:objects obj-id :y-pos] y))))
