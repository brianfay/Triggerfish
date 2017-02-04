(ns triggerfish.client.utils.macros)

(defmacro deftouchable [tname args & remaining-args]
  "Defines a reagent component with hammerjs touch functionality. Give it a name, followed by arguments to the component, and forms to define the behavior of the component.
Everything up to the last form will be used as the :component-did-mount (add touch-handlers to the ham-man).
The last form should be a function to use as the reagent-render."
  (let [touch-handlers (butlast remaining-args)
        render-fn      (last remaining-args)]
    `(defn ~tname ~args
      (let [ham-man-atom# (atom nil)]
        (reagent.core/create-class
         {:component-will-unmount
          (fn [this#]
            (when-let [~'ham-man @ham-man-atom#]
              (.destroy ~'ham-man)))
          :component-did-mount
          (fn [this#]
            (let [~'dom-node (reagent.core/dom-node this#)
                  ~'ham-man  (triggerfish.client.utils.hammer/hammer-manager ~'dom-node)]
              (reset! ham-man-atom# ~'ham-man)
              ~@touch-handlers))
          :reagent-render ~render-fn})))))
