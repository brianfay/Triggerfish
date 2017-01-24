(ns triggerfish.server.object.core
  (:require
   [triggerfish.server.id-allocator :as id-alloc]))

;;an atom of all object types (not running object instances)
(defonce object-registry (atom {}))

;;an atom of private state used by running object instances
(defonce private-object-state (atom {}))

(defn register-object [obj-name obj-map]
  (swap! object-registry assoc obj-name obj-map))

(defn get-public-obj-def [obj-type]
  "Returns info from the object-registry that makes sense to send over the wire (excludes functions)"
  (let [obj-def (get @object-registry obj-type)
        {:keys [controls inlets outlets]} obj-def]
    {:type obj-type
     :name obj-type
     :controls (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type :val]))) {} controls)
     :inlets   (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type]))) {} inlets)
     :outlets  (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type]))) {} outlets)}))

(defn get-public-obj-defs []
  (let [ks (keys @object-registry)]
    (zipmap ks (map get-public-obj-def ks))))

(defn symbols-binding->keywords-binding [bindings]
  (let [partitioned-bindings (partition 2 bindings)
        ks                   (map (comp keyword first) partitioned-bindings)
        vals                 (map second partitioned-bindings)]
    (vec (interleave ks vals))))

(defn set-private-object-state [obj-id locals]
  (swap! private-object-state assoc obj-id locals))

(defn update-private-state-if-bindings-provided [obj-id return-val]
  (when (and (vector? return-val) (even? (count return-val)))
    (swap! private-object-state (fn [m] (update-in m [obj-id] #(apply assoc % return-val))))))

(defn get-private-object-state [obj-id]
  (get @private-object-state obj-id))

(defn dissoc-private-object-state [obj-id]
  (swap! private-object-state dissoc obj-id))

(defn create-object [obj-type]
  "Constructs a new object of the given type and returns an 'obj-map' of public information about the object."
  (let [obj-id     (id-alloc/new-node-id)
        obj-def    (get @object-registry obj-type)
        {:keys [constructor controls inlets outlets]} obj-def]
    (constructor obj-id)
    {:type obj-type
     :name obj-type ;;might be cool to have object be individually nameable some day, I'll leave this here for now
     :obj-id   obj-id
     :controls (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type :val]))) {} controls)
     :inlets   (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type]))) {} inlets)
     :outlets  (reduce (fn [acc [k v]] (assoc acc k (select-keys v [:type]))) {} outlets)}))

(defn control-object [obj-map control-name val]
  "Sets a control on an object by calling its control function. Returns a map with updated controls."
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        ctl-fn  (get-in obj-def [:controls control-name :fn])]
    (ctl-fn obj-id val)
    (assoc-in obj-map [:controls control-name :value] val)))

(defn connect-inlet [obj-map inlet-name bus]
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        {:keys [inlets]} obj-def
        inlet (get inlets inlet-name)
        connect-fn (:connect inlet)]
    (connect-fn obj-id bus)
    nil))

(defn disconnect-inlet [obj-map inlet-name]
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        {:keys [inlets]} obj-def
        inlet (get inlets inlet-name)
        disconnect-fn (:disconnect inlet)]
    (disconnect-fn obj-id)))

(defn connect-outlet [obj-map outlet-name bus]
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        {:keys [outlets]} obj-def
        outlet (get outlets outlet-name)
        connect-fn (:connect outlet)]
    (connect-fn obj-id bus)
    nil))

(defn disconnect-outlet [obj-map outlet-name]
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        {:keys [outlets]} obj-def
        outlet (get outlets outlet-name)
        disconnect-fn (:disconnect outlet)]
    (disconnect-fn obj-id)))

(defn destroy-object [obj-map]
  "Given an object and its type, removes it from the server"
  (let [{:keys [obj-id type]} obj-map
        obj-def (get @object-registry type)
        destructor (:destructor obj-def)]
    (destructor obj-id)
    (id-alloc/free-node-id obj-id)
    nil))
