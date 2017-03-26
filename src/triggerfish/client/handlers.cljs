(ns triggerfish.client.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx debug trim-v]]
            [triggerfish.client.sente-events :refer [chsk-send!]]))

(def standard-interceptors [trim-v #_debug])

(def initial-state
  {:objects  {}
   :obj-defs nil
   :num-moving-objects 0
   :pan      {:x-pos 0 :y-pos 0 :offset-x 0 :offset-y 0}
   :zoom     {:current-zoom 1 :scale 1}
   :menu     {:visibility        true
              :current-view      :main-menu
              :inspected-object  nil
              :inspected-control nil}})

(defn assoc-if [m pred & kvs]
  (if pred
    (apply assoc (concat [m] kvs))
    m))

(defn inlet-connected?
  [db obj-id inlet-name]
  (not (empty? (filter #(= (first %) [obj-id inlet-name]) (:connections db)))))

;;(assoc db :selected-outlet [obj-id outlet-name type])
(defn inlet-connected-to-selected-outlet?
  [db obj-id inlet-name]
  (let [[out-obj-id outlet-name _] (:selected-outlet db)]
    (= (get (:connections db) [obj-id inlet-name])) [out-obj-id outlet-name]))

(reg-event-db
 :initialize
 standard-interceptors
 (fn [db _]
   (merge db initial-state)))

(reg-event-db
 :patch-recv
 standard-interceptors
 (fn [db [patch]]
   (let [objects (dissoc patch :connections)
         connections (:connections patch)]
     (assoc db :objects objects :connections connections))))

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
    [:obj/connect params])))

(reg-fx
 :disconnect
 (fn [params]
   (chsk-send!
    [:obj/disconnect params])))

(reg-fx
 :move-object
 (fn [[obj-id x y]]
   (chsk-send!
    [:obj/move
     {:obj-id obj-id
      :x-pos x
      :y-pos y}])))

;;when objects are being dragged, need to prevent dragging the patch canvas.
;;for touch we can just check the target of the pan event, but for mouse it's picking up the
(reg-event-db
 :start-moving-object
 (fn [db]
   (update db :num-moving-objects inc)))

(reg-event-db
 :stop-moving-object
 (fn [db]
   (update db :num-moving-objects dec)))

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
     (if (and (or (nil? selected-outlet)
                  (inlet-connected-to-selected-outlet? db obj-id inlet-name))
              (inlet-connected? db obj-id inlet-name))
       {:disconnect  {:in-id obj-id :in-name inlet-name}
        :dispatch    [:ghost-disconnect obj-id inlet-name]}
       (if (and selected-outlet (not= out-obj-id obj-id) (= type outlet-type))
         {:connect  {:in-id obj-id :in-name inlet-name :out-id out-obj-id :out-name outlet-name}
          :dispatch [:ghost-connect obj-id inlet-name out-obj-id outlet-name]})))))

(reg-event-db
 :ghost-connect
 standard-interceptors
 (fn [db [obj-id inlet-name out-obj-id outlet-name]]
   (assoc-in db [:connections [obj-id inlet-name]] [out-obj-id outlet-name])))

(reg-event-db
 :ghost-disconnect
 standard-interceptors
 (fn [db [obj-id inlet-name]]
   (update-in db [:connections] #(dissoc % [obj-id inlet-name]))))

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
   (if (= 0 (:num-moving-objects db))
     (-> db
        (assoc-in [:pan :offset-x] (+ (:x-pos db) delta-x))
        (assoc-in [:pan :offset-y] (+ (:y-pos db) delta-y)))
     db)))

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
   (chsk-send! [:obj/create
                {:name obj-name
                 :x-pos x
                 :y-pos y}])))

(reg-event-db      ;;a mock object for optimistic ui updates
 :add-ghost-object
 standard-interceptors
 (fn [db [selected-add-obj x y]]
   (let [obj-def (get (:obj-defs db) selected-add-obj)]
     (assoc-in db [:objects (str "ghost-" (rand-int 100000))]
               (merge
                obj-def
                {:name selected-add-obj :x-pos x :y-pos y})))))

(reg-fx
 :delete-object
 (fn [[obj-id]]
   (chsk-send! [:obj/delete
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

(def opening-menu-state
  {:visibility   true
   :current-view :main-menu})

(def closing-menu-state
  {:visibility false})

(reg-event-fx
 :app-container-clicked
 standard-interceptors
 (fn [{:keys [db]} [x y]]
   (let [visible?            (get-in db [:menu :visibility])
         current-view          (get-in db [:menu :current-view])
         [scaled-x scaled-y] (translate-and-scale-points db x y)
         selected-add-obj    (get-in db [:menu :selected-add-obj])
         menu-state (-> (:menu db)
                        (assoc :current-view :main-menu)
                        (assoc-if (not visible?) :visibility true)
                        (assoc-if (and visible? (= current-view :main-menu) (nil? selected-add-obj)) :visibility false)
                        )]
     (merge
      {:db (update db :menu #(merge % menu-state))}
      (when (and selected-add-obj (= current-view :main-menu))
        {:add-object [selected-add-obj scaled-x scaled-y]
         :dispatch   [:add-ghost-object     selected-add-obj scaled-x scaled-y]})))))

(defn get-object-connections [db obj-id]
  "Returns [in-id inlet-name] of all connections involving a given object id"
  (reduce
   (fn [acc [[in-id inlet-name] [out-id outlet-name]]]
     (if (or (= obj-id in-id) (= obj-id out-id))
       (conj acc [in-id inlet-name])
       acc))
   []
   (:connections db)))

(reg-event-db
 :object-header-clicked
 standard-interceptors
 (fn [{:keys [menu] :as db} [obj-id]]
   (if (and (:visibility menu) (= :obj-inspector (:current-view menu)) (= obj-id (:inspected-object menu)))
     (update db :menu #(assoc % :visibility false))
     (update db :menu #(assoc % :visibility true, :current-view :obj-inspector, :inspected-object obj-id)))))

(reg-event-db
 :inspect-control
 standard-interceptors
 (fn [db [obj-id ctl-name]]
   (update db :menu #(assoc % :inspected-control [obj-id ctl-name]
                              :current-view :control-inspector))))

(reg-event-fx
 :delete-object
 standard-interceptors
 (fn [{:keys [db]} [obj-id]]
   (let [connections-to-remove (get-object-connections db obj-id)]
     {:delete-object [obj-id]
      :db (-> db
              (assoc-in [:menu :current-view] :main-menu)
              (update-in [:objects] #(dissoc % obj-id))
              (update :connections #(apply dissoc % connections-to-remove)))})))

(reg-event-db
 :close-menu
 standard-interceptors
 (fn [db]
   (assoc-in db [:menu :visibility] false)))

(reg-event-db
 :select-add-obj
 standard-interceptors
 (fn [db [obj-name]]
   (if (not= obj-name (get-in db [:menu :selected-add-obj]))
     (assoc-in db [:menu :selected-add-obj] obj-name)
     (assoc-in db [:menu :selected-add-obj] nil))))

;; Controls
(reg-fx
 :set-control
 (fn [ctl-params] ;;obj-id ctl-name val
   (chsk-send!
    [:obj/set-control ctl-params])))

(defn clip [min max x]
  (cond (> x max) max
        (< x min) min
        :else     x))

(defn calc-new-dial-val [db obj-id ctl-name delta-y]
  (let [init-val (get-in db [:init-dial-val obj-id ctl-name])
        {:keys [min max]} (get-in db [:objects obj-id :controls ctl-name :params])
        dial-range (- max min)
        percent-moved (/ delta-y 500) ;;500px seems like a good distance to move a dial 100% in either direction
        movement (* dial-range percent-moved)]
    (clip min max (- init-val movement))))

(reg-event-db
 :update-control
 standard-interceptors
 (fn [db [[obj-id ctl-name val]]]
   (assoc-in db [:objects obj-id :controls ctl-name :val] val)))

(reg-event-fx
 :start-moving-dial
 standard-interceptors
 (fn [{:keys [db]} [obj-id ctl-name init-val delta-y]]
   (let [new-val (calc-new-dial-val db obj-id ctl-name delta-y)]
     {:set-control [obj-id ctl-name new-val]
      :db (-> db
              (assoc-in [:init-dial-val obj-id ctl-name] init-val)
              (assoc-in [:objects obj-id :controls ctl-name :val] new-val))})))

(reg-event-fx
 :move-dial
 standard-interceptors
 (fn [{:keys [db]} [obj-id ctl-name delta-y]]
   (let [new-val (calc-new-dial-val db obj-id ctl-name delta-y)]
     {:set-control [obj-id ctl-name new-val]
      :db (assoc-in db [:objects obj-id :controls ctl-name :val] new-val)})))

(reg-event-fx
 :stop-moving-dial
 standard-interceptors
 (fn [{:keys [db]} [obj-id ctl-name delta-y]]
   (let [new-val (calc-new-dial-val db obj-id ctl-name delta-y)]
     {:set-control [obj-id ctl-name new-val]
      :db (-> db
              (assoc-in  [:objects obj-id :controls ctl-name :val] new-val)
              (update-in [:init-dial-val obj-id] #(dissoc % ctl-name)))})))

;;MIDI
(reg-event-db
 :midi/fiddled
 standard-interceptors
 (fn [db [recently-fiddled]]
   (assoc db :recently-fiddled recently-fiddled)))
