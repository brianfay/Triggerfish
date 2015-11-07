(ns triggerfish.components
  (:require [sablono.core :as sab]))

(defn like-seymore [data]
  (sab/html [:div
             [:h1 "Brian's exceptional popularity: " (:likes @data)]
             [:div [:h1 {
                         :onClick #(swap! data update-in [:likes] inc)}
                    "Thumbs way the heck up!"]]]))
