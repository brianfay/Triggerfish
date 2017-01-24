(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx debug trim-v]]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

(def standard-interceptors [trim-v #_debug])

(def initial-state
  {:objects  {}
   :obj-defs nil
   :pan      {:x-pos 0 :y-pos 0 :offset-x 0 :offset-y 0}
   :zoom     {:current-zoom 1 :scale 1}
   :menu     {:visibility false
             :selected-action nil}})

(defn inlet-connected?
  [db obj-id inlet-name]
  (not (empty? (filter #(= (first %) [obj-id inlet-name]) (get-in db [:objects :connections])))))

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

(reg-event-db
 :obj-defs
 standard-interceptors
 (fn [db [obj-defs]]
   (assoc db :obj-defs obj-defs)))

;;Objects:

(reg-fx
 :connect
 (fn [params]
   (chsk-send!
    [:patch/connect params])))

(reg-fx
 :disconnect
 (fn [params]
   (chsk-send!
    [:patch/disconnect params])))

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
         [out-obj-id outlet-name outlet-type] selected-outlet]
     (if (and (nil? selected-outlet) (inlet-connected? db obj-id inlet-name))
       {:disconnect  {:in-id obj-id :in-name inlet-name}
        :dispatch    [:ghost-disconnect obj-id inlet-name]}
       (if (and selected-outlet (not= out-obj-id obj-id) (= type outlet-type))
         {:connect  {:in-id obj-id :in-name inlet-name :out-id out-obj-id :out-name outlet-name}
          :dispatch [:ghost-connect obj-id inlet-name out-obj-id outlet-name]})))))

(reg-event-db
 :ghost-connect
 standard-interceptors
 (fn [db [obj-id inlet-name out-obj-id outlet-name]]
   (assoc-in db [:objects :connections [obj-id inlet-name]] [out-obj-id outlet-name])))

(reg-event-db
 :ghost-disconnect
 standard-interceptors
 (fn [db [obj-id inlet-name]]
   (update-in db [:objects :connections] #(dissoc % [obj-id inlet-name]))))

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

(reg-event-db
 :register-object-width
 standard-interceptors
 (fn [db [obj-id width]]
   (assoc-in db [:object-widths obj-id] width)))

(reg-event-db ;;vertical offset from top of object element to middle of inlet
 :register-inlet-offset
 standard-interceptors
 (fn [db [obj-id inlet-name offset]]
   (assoc-in db [:inlet-offsets obj-id inlet-name] offset)))

(reg-event-db ;;vertical offset from top of object element to middle of outlet
 :register-outlet-offset
 standard-interceptors
 (fn [db [obj-id outlet-name offsetTop]]
   (assoc-in db [:outlet-offsets obj-id outlet-name] offsetTop)))

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
   (let [obj-def (get (:obj-defs db) selected-obj)]
     (assoc-in db [:objects (str "ghost-" (rand-int 100000))]
               (merge
                obj-def
                {:name selected-obj :x-pos x :y-pos y})))))

(reg-fx
 :delete-object
 (fn [[obj-id]]
   (chsk-send! [:patch/delete-object
                {:obj-id obj-id}])))

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
   (let [selected-action     (get-in db [:menu :selected-action])
         visible?            (get-in db [:menu :visibility])
         [scaled-x scaled-y] (translate-and-scale-points db x y)
         selected-obj        (get-in db [:menu :selected-obj])]
     (merge
      (when (= selected-action nil) {:db (assoc-in db [:menu :visibility] (not visible?))})
      (if (and (= selected-action "add") selected-obj)
        {:add-object [selected-obj scaled-x scaled-y]
         :dispatch   [:add-ghost-object     selected-obj scaled-x scaled-y]}
        {:dispatch   [:deselect-outlet]})))))

(defn get-object-connections [db obj-id]
  "Returns [in-id inlet-name] of all connections involving a given object id"
  (reduce
   (fn [acc [[in-id inlet-name] [out-id outlet-name]]]
     (if (or (= obj-id in-id) (= obj-id out-id))
       (conj acc [in-id inlet-name])
       acc))
   []
   (get-in db [:objects :connections])))

(reg-event-fx
 :object-clicked
 standard-interceptors
 (fn [{:keys [db]} [obj-id]]
   (let [connections-to-remove (get-object-connections db obj-id)]
     (if (= "delete" (get-in db [:menu :selected-action]))
       {:delete-object [obj-id]
        :db (-> db
                (update-in [:objects] #(dissoc % obj-id))
                (update-in [:objects :connections] #(apply dissoc % connections-to-remove)))}
       {}))))

(reg-event-db
 :select-action
 standard-interceptors
 (fn [db [action]]
   (let [current-action (get-in db [:menu :selected-action])]
     (assoc-in db [:menu :selected-action] (when (not= current-action action) action)))))

(reg-event-db
 :close-menu
 standard-interceptors
 (fn [db]
   (assoc-in db [:menu :visibility] false)))

(reg-event-db
 :select-obj-to-insert
 standard-interceptors
 (fn [db [obj-name]]
   (if (not= obj-name (get-in db [:menu :selected-obj]))
     (assoc-in db [:menu :selected-obj] obj-name)
     (assoc-in db [:menu :selected-obj] nil))))
