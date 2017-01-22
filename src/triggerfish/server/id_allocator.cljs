(ns triggerfish.server.id-allocator)

(def buffer-range (range 1024))
(def obj-id-range (range 2 (.-MAX-VALUE js/Number)))
(def node-id-range (range 2 (.-MAX-VALUE js/Number)))

(def buffer-ids-in-use (atom #{}))
(def obj-ids-in-use (atom #{}))
(def node-ids-in-use (atom #{}))

(defn- alloc-new-id
  [rng in-use]
  (let [new-id (some #(when-not (contains? @in-use %) %) rng)]
    (swap! in-use conj new-id)
    new-id))

(defn- dealloc-id
  [in-use id]
  (swap! in-use disj id))

(defn new-buffer-id
  []
  (alloc-new-id buffer-range buffer-ids-in-use))

(defn free-buffer-id
  [id]
  (dealloc-id buffer-ids-in-use id))

(defn new-obj-id
  []
  (alloc-new-id obj-id-range obj-ids-in-use))

(defn free-obj-id
  [id]
  (dealloc-id obj-ids-in-use id))

(defn new-node-id
  []
  (alloc-new-id node-id-range node-ids-in-use))

(defn free-node-id
  [id]
  (dealloc-id node-ids-in-use id))
