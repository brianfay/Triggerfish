(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [reg-event]]
            [triggerfish.shared.object-definitions :as obj]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

(def initial-state
  {:objects {}
   :connections {}
   :positions {}
   :toolbar-hidden true
   :mode :insert
   :selected-outlet [nil nil]
   :obj-name-last-click {}
   })

(reg-event
 :initialize
 (fn
   [db _]
   (merge db initial-state)))

(reg-event
 :patch-recv
 (fn [db [ev-id patch]]
   (assoc db :objects (dissoc patch :connections) :connections (:connections patch))))

;;Updates the app-db with the DOM position of an inlet or outlet
(reg-event
 :update-io-position
 (fn [db [ev-id obj-id inlet-or-outlet-name pos]]
   (assoc-in db [:positions [obj-id inlet-or-outlet-name]] pos)))

(reg-event
 :update-object-dom-position
 (fn [db [ev-id obj-id pos]]
   (assoc-in db [:positions obj-id] pos)))

(reg-event
 :dissoc-position
 (fn [db [ev-id obj-id inlet-or-outlet-name]]
   (update-in db [:positions] dissoc [obj-id inlet-or-outlet-name])))

(reg-event
 :move-object
 (fn [db [ev-id obj-id x y]]
   (do
     (chsk-send! [:patch/move-object {:obj-id obj-id :x-pos x :y-pos y}])
     (-> db
         (assoc-in [:objects obj-id :x-pos] (if (> x 10) x 10))
         (assoc-in [:objects obj-id :y-pos] (if (> y 0) y 0))))))

(reg-event
 :select-create-object
 (fn [db [ev-id name]]
   (assoc db :selected-create-object name)))

(reg-event
 :close-control-window
 (fn [db [ev-id]]
   (assoc db :selected-control-object nil)))

(reg-event
 :select-control-object
 (fn [db [ev-id obj-id]]
   (assoc db :selected-control-object obj-id)))

(reg-event
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

(reg-event
 :optimistic-delete
 (fn [db [ev-id obj-id]]
   (let [connections (:connections db)
         connections-to-remove (keys (merge (into {} (get-connected-inlets connections obj-id))
                                            (into {} (get-connected-outlets connections obj-id))))]
     (-> (reduce #(update-in %1 [:connections] dissoc %2) db connections-to-remove)
         (update-in [:objects] dissoc obj-id)
         (update :selected-control-object #(if (= % obj-id) nil %))))))

(reg-event
 :connect
 (fn [db [ev-id inlet-id inlet-name type]]
   (let [selected-outlet (:selected-outlet db)
         [outlet-id outlet-name outlet-type] selected-outlet]
     (when (not (= inlet-id outlet-id))
       (if (and (not (nil? selected-outlet)) (= type outlet-type))
         (do
           (chsk-send! [:patch/connect {:in-id inlet-id :in-name inlet-name :out-id outlet-id :out-name outlet-name}])
           (assoc-in db [:connections [inlet-id inlet-name]] [outlet-id outlet-name]))
         (if (not (nil? (get (:connections db) [inlet-id inlet-name])))
           (do
             (chsk-send! [:patch/disconnect {:in-id inlet-id :in-name inlet-name}])
             (update-in db [:connections] dissoc [inlet-id inlet-name]))
           db))))))

(reg-event
 ;;select an outlet
 :select-outlet
 (fn [db [ev-id [id name type]]]
   (let [prev-outlet (:selected-outlet db)]
     (if (not (= prev-outlet [id name type]))
       (assoc db :selected-outlet [id name type])
       (dissoc db :selected-outlet)))))

(reg-event
 :select-object
 (fn [db [ev-id obj-id]]
   (if (not (= obj-id (first (:selected-outlet db))))
     (assoc db :selected-outlet [obj-id nil nil])
     db)))

(reg-event
 :deselect-outlet
 (fn [db [ev-id [id name type]]]
   (dissoc db :selected-outlet)))

(reg-event
 :min-max
 (fn [db [ev-id obj-id current-val]]
   (assoc-in db [:minimized obj-id] (not current-val))))

(reg-event
 :set-mode
 (fn [db [ev-id mode]]
   (if (= mode (:mode db))
     (assoc db :mode nil)
     (assoc db :mode mode))))

(reg-event
 :open-toolbar
 (fn [db [ev-id]]
   (assoc db :toolbar-hidden false)))

(reg-event
 :close-toolbar
 (fn [db [ev-id]]
   (assoc db :toolbar-hidden true)))

(reg-event
 :update-patch-size
 (fn [db [ev-id width height]]
   (assoc db :patch-size [width height])))

(reg-event
 :optimistic-set-control
 (fn [db [ev-id obj-id ctrl-name value]]
   (assoc-in db [:objects obj-id :controls ctrl-name :value] value)))

(reg-event
 :click-obj-name
 (fn [db [ev-id id]]
   (let [now (.now js/Date)
         prev-click (get-in db [:obj-name-last-click id])]
     (if (< (- now prev-click) 300)
       (-> db
           (assoc-in [:obj-name-last-click id] now)
           (update-in [:minimized id] not))
       (assoc-in db [:obj-name-last-click id] now)))))
