(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [register-handler]]
            [triggerfish.shared.object-definitions :as obj]))

(def initial-state
  {:objects {}
   :connections {}
   :positions {}
   :sidebar-open true
   :mode :insert
   :selected-io [nil nil]
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
 :update-io-position
 (fn [db [ev-id obj-id inlet-or-outlet-name pos]]
   (assoc-in db [:positions [obj-id inlet-or-outlet-name]] pos)))

(register-handler
 :update-object-position
 (fn [db [ev-id obj-id pos]]
   (assoc-in db [:positions obj-id] pos)))

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
   (let [id (str "ghost" (rand-int 99999))]
     (assoc-in db [:objects id]
              (merge (obj-name obj/objects)
                     {:id id
                      :x-pos x-pos
                      :y-pos y-pos
                      :name obj-name
                      :optimistic true})))))

(defn get-connected-inlets
  [connections obj-id]
  (filter #(= obj-id (first (first %))) connections))

(defn get-connected-outlets
  [connections obj-id]
  (filter #(= obj-id (first (second %))) connections))

(register-handler
 :optimistic-delete
 (fn [db [ev-id obj-id]]
   (let [connections (:connections db)
         connections-to-remove (keys (merge (into {} (get-connected-inlets connections obj-id))
                                            (into {} (get-connected-outlets connections obj-id))))]
     (-> (reduce #(update-in %1 [:connections] dissoc %2) db connections-to-remove)
         (update-in [:objects] dissoc obj-id)))))

(register-handler
 :connect
 (fn [db [ev-id [id i-or-o name]]]
   (let [prev-selected (:selected-io db)
         [p-id p-i-or-o p-name] prev-selected]
     (if (= i-or-o :inlet)
       (assoc-in db [:connections [id name]] [p-id p-name])
       (assoc-in db [:connections [p-id p-name]] [id name])))))

(register-handler
 ;;select an inlet or outlet
 :select-io
 (fn [db [ev-id [i-or-o name type]]]
   (assoc db :selected-io [(first (:selected-io db)) i-or-o name type])))

(register-handler
 :select-object
 (fn [db [ev-id obj-id]]
   (if (not (= obj-id (first (:selected-io db))))
     (assoc db :selected-io [obj-id nil nil])
     db)))

(register-handler
 :min-max
 (fn [db [ev-id obj-id current-val]]
   (assoc-in db [:minimized obj-id] (not current-val))))

(register-handler
 :set-mode
 (fn [db [ev-id mode]]
   (assoc db :mode mode)))

(register-handler
 :open-sidebar
 (fn [db [ev-id]]
   (assoc db :sidebar-open true)))

(register-handler
 :close-sidebar
 (fn [db [ev-id]]
   (assoc db :sidebar-open false)))
