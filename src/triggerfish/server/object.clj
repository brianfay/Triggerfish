(ns triggerfish.server.object)

(defmacro defobject
  "Registers an object and its implementation. Expected arguments are a name, constructor, destructor, and optionally inlets, outlets, and controls.
  NB: Doesn't actually def it to a var, but using def in the name
  helps with cider/clj-fmt formatting"
  [object-name & forms]
  (let [constructor-form (first (filter #(= (first %) 'constructor) forms))
        destructor-form  (first (filter #(= (first %) 'destructor) forms))
        inlet-forms      (filter #(contains? #{'inlet-ar 'inlet-kr} (first %)) forms)
        outlet-forms     (filter #(contains? #{'outlet-ar 'outlet-kr} (first %)) forms)
        control-forms    (filter #(= (first %) 'control) forms)]
    `(triggerfish.server.objects/register-object
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
  [bindings & body]
  `(fn [obj-id#]
     (let ~bindings
       (do
         (triggerfish.server.objects/set-private-object-state obj-id# (locals))
         ~@body))))

(defmacro destructor
  "Returns a function that can be used to destruct an object.
  First arg should be a vector of symbols to import from the object's private state."
  [imports & body]
  (let [imports-list (into (list) imports)]
    `(fn [obj-id#]
       (let [{:keys [~@imports-list]} (triggerfish.server.objects/get-private-object-state obj-id#)]
         (triggerfish.server.objects/dissoc-private-object-state obj-id#)
         (do ~@body)))))

(defmacro control
  "Returns a function that can be used to control an object. The control value of this function will be bound to the symbol 'val.
  First arg is a vector of desired symbols to import from the object's private state. Second arg is the initial value of the control.
  When the return of the body is a vector of keyword-value pairs, these will be assoc'ed into the object's private state."
  [control-name init-val imports & body]
  (let [imports-list         (into (list) imports)
        has-bindings-vector? (vector? (last body))]
    `{:name ~control-name
      :val  ~init-val
      :type :type-is-currently-unused
      :fn (fn [obj-id# val#]
            (let [~'val    val#
                  return# (let [{:keys [~@imports-list]} (triggerfish.server.objects/get-private-object-state obj-id#)]
                            (do ~@body))]
              (triggerfish.server.objects/update-private-state-if-bindings-provided obj-id# return#)))}))

(defmacro in-or-out-let
  "Returns a map with the type of an inlet or outlet, a connect function, and a disconnect function.
  Connect and disconnect may optionally return a vector of bindings to assoc into the object's private state"
  [type in-or-out-name imports & callback-pairs]
  (let [imports-list      (into (list) imports)
        partitioned-cb    (partition 2 callback-pairs)
        [_ connect-cb]    (first (filter (fn [[kw fun]] (= kw :connect))    partitioned-cb))
        [_ disconnect-cb] (first (filter (fn [[kw fun]] (= kw :disconnect)) partitioned-cb))]
    `{:name ~in-or-out-name
      :type ~type
      :connect    (fn [obj-id# bus#]
                    (let [{:keys [~@imports-list]} (triggerfish.server.objects/get-private-object-state obj-id#)]
                      (triggerfish.server.objects/update-private-state-if-bindings-provided
                        obj-id#
                        (~connect-cb bus#))))
      :disconnect (fn [obj-id#]
                    (let [{:keys [~@imports-list]} (triggerfish.server.objects/get-private-object-state obj-id#)]
                      (triggerfish.server.objects/update-private-state-if-bindings-provided
                       obj-id#
                       (~disconnect-cb))))}))

(defmacro outlet-ar
  "Audio rate outlet"
  [outlet-name imports & callback-pairs]
  `(in-or-out-let :audio ~outlet-name ~imports ~@callback-pairs))

(defmacro outlet-kr [outlet-name imports & callback-pairs]
  "Control rate outlet"
  `(in-or-out-let :control ~outlet-name ~imports ~@callback-pairs))

(defmacro inlet-ar
  "Audio rate inlet"
  [inlet-name imports & callback-pairs]
  `(in-or-out-let :audio ~inlet-name ~imports ~@callback-pairs))

(defmacro inlet-kr
  "Control rate inlet"
  [inlet-name imports & callback-pairs]
  `(in-or-out-let :control ~inlet-name ~imports ~@callback-pairs))
