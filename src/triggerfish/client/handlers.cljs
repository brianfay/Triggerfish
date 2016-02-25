(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [register-handler]]
            [triggerfish.shared.object-definitions :as obj]))

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
 :dissoc-position
 (fn [db [ev-id obj-id inlet-or-outlet-name]]
   (update-in db [:positions] dissoc [obj-id inlet-or-outlet-name])))

(register-handler
 :move-object
 (fn [db [ev-id obj-id x y]]
   (-> db
    (assoc-in [:objects obj-id :x-pos] x)
    (assoc-in [:objects obj-id :y-pos] y))))

(register-handler
 :select-create-object
 (fn [db [ev-id name]]
   (assoc db :selected-create-object name)))

(register-handler
 :optimistic-create
 (fn [db [ev-id obj-name x-pos y-pos]]
   ;;todo - guarantee id is unique
   (let [id (str "ghost" (rand-int 99999))](assoc-in db [:objects id]
              (merge (obj-name obj/objects)
                     {:id id
                      :x-pos x-pos
                      :y-pos y-pos
                      :name obj-name
                      :optimistic true})))))

(register-handler
 :set-mode
 (fn [db [ev-id mode]]
   (assoc db :mode mode)))
