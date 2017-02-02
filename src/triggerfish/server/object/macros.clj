(ns triggerfish.server.object.macros)

(defmacro defobject
  "Registers an object and its implementation. Expected arguments are a name, constructor, destructor, and optionally inlets, outlets, and controls.
  NB: Doesn't actually def it to a var, but using def in the name
  helps with cider/clj-fmt formatting"
  [object-name & forms]
  (let [constructor-form (first (filter #(contains? #{'constructor 'simple-constructor} (first %)) forms))
        destructor-form  (first (filter #(contains? #{'destructor 'simple-destructor} (first %)) forms))
        inlet-forms      (filter #(contains? #{'inlet-ar 'inlet-kr 'simple-inlet-ar 'simple-inlet-kr} (first %)) forms)
        outlet-forms     (filter #(contains? #{'outlet-ar 'outlet-kr 'simple-outlet-ar 'simple-outlet-kr} (first %)) forms)
        control-forms    (filter #(contains? #{'control 'simple-control} (first %)) forms)]
    `(triggerfish.server.object.core/register-object
      (keyword '~object-name)
      {:constructor ~constructor-form
       :destructor  ~destructor-form
       :inlets      (reduce (fn [acc# m#]
                              (assoc acc# (:name m#) (dissoc m# :name)))
                            {}
                            [~@inlet-forms])
       :outlets     (reduce (fn [acc# m#]
                              (assoc acc# (:name m#) (dissoc m# :name)))
                            {}
                            [~@outlet-forms])
       :controls    (reduce (fn [acc# m#]
                              (assoc acc# (:name m#) (dissoc m# :name)))
                            {}
                            [~@control-forms])})))

(defmacro locals
  "Gets the local bindings in scope, including some gensym'd stuff we probably don't need."
  []
  (let [ks (keys (:locals &env))]
    `(zipmap (map keyword '~ks) [~@ks])))

(defmacro constructor
  "Returns a function that can be used to construct an object.
  Bindings are saved so that they can be imported in the destructor, controls, etc"
  [& body]
  (let [bindings (when (vector? (first body)) (first body))]
    (if (not-empty bindings)
      `(fn [{:keys [~'obj-id] :as ~'obj-map}]
         (let ~bindings
           (do ~@(rest body)
               (triggerfish.server.object.core/set-private-object-state ~'obj-id (locals)))))
      `(fn [{:keys [~'obj-id] :as ~'obj-map}]
         (do ~@body
             (triggerfish.server.object.core/set-private-object-state ~'obj-id nil))))))

(defmacro destructor
  "Returns a function that can be used to destruct an object.
  First arg should be a vector of symbols to import from the object's private state."
  [& body]
  (let [imports-list (when (vector? (first body)) (into (list) (first body)))]
    (if (not-empty imports-list)
      `(fn [{:keys [~'obj-id] :as ~'obj-map}]
         (let [{:keys [~@imports-list]} (triggerfish.server.object.core/get-private-object-state ~'obj-id)]
           (triggerfish.server.object.core/dissoc-private-object-state ~'obj-id)
           (do ~@(rest body))))
      `(fn [{:keys [~'obj-id] :as ~'obj-map}]
         (triggerfish.server.object.core/dissoc-private-object-state ~'obj-id)
         (do ~@(rest body))))))

(defmacro control
  "Returns a function that can be used to control an object. The control value of this function will be bound to the symbol 'val.
  First (optional) arg is a vector of desired symbols to import from the object's private state. Second arg is the initial value of the control.
  When the return of the body is a vector of keyword-value pairs, these will be assoc'ed into the object's private state."
  [control-name type init-val & body]
  (let [imports-list (when (vector? (first body)) (into (list) (first body)))
        body         (if (not-empty imports-list) (rest body) body)]
    `{:name ~control-name
      :val  ~init-val
      :type ~type
      :fn (fn [{:keys [~'obj-id] :as ~'obj-map} val#]
            (let [~'val   val#
                  return# (let [{:keys [~@imports-list]} (triggerfish.server.object.core/get-private-object-state ~'obj-id)]
                            (do ~@body))]
              (triggerfish.server.object.core/update-private-state-if-bindings-provided ~'obj-id return#)))}))

(defmacro in-or-out-let
  "Returns a map with the type of an inlet or outlet, a connect function, and a disconnect function.
  Connect and disconnect may optionally return a vector of bindings to assoc into the object's private state"
  [type in-or-out-name & callback-pairs]
  (let [imports-list      (when (vector? (first callback-pairs)) (into (list) (first callback-pairs)))
        callback-pairs    (if (not-empty imports-list) (rest callback-pairs) callback-pairs)
        partitioned-cb    (partition 2 callback-pairs)
        [_ connect-cb]    (first (filter (fn [[kw fun]] (= kw :connect))    partitioned-cb))
        [_ disconnect-cb] (first (filter (fn [[kw fun]] (= kw :disconnect)) partitioned-cb))]
    `{:name ~in-or-out-name
      :type ~type
      :connect    (fn [{:keys [~'obj-id] :as ~'obj-map} bus#]
                    (let [{:keys [~@imports-list]} (triggerfish.server.object.core/get-private-object-state ~'obj-id)]
                      (triggerfish.server.object.core/update-private-state-if-bindings-provided
                        ~'obj-id
                        (~connect-cb bus#))))
      :disconnect (fn [{:keys [~'obj-id] :as ~'obj-map}]
                    (let [{:keys [~@imports-list]} (triggerfish.server.object.core/get-private-object-state ~'obj-id)]
                      (triggerfish.server.object.core/update-private-state-if-bindings-provided
                       ~'obj-id
                       (~disconnect-cb))))}))

(defmacro outlet-ar
  "Audio rate outlet"
  [outlet-name & callback-pairs]
  `(in-or-out-let :audio ~outlet-name ~@callback-pairs))

(defmacro outlet-kr [outlet-name & callback-pairs]
  "Control rate outlet"
  `(in-or-out-let :control ~outlet-name ~@callback-pairs))

(defmacro inlet-ar
  "Audio rate inlet"
  [inlet-name & callback-pairs]
  `(in-or-out-let :audio ~inlet-name ~@callback-pairs))

(defmacro inlet-kr
  "Control rate inlet"
  [inlet-name & callback-pairs]
  `(in-or-out-let :control ~inlet-name ~@callback-pairs))
