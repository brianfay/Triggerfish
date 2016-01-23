(ns triggerfish.client.core
  (:require [rum.core :as rum]))

(rum/defc label [n text]
  [:div
   [:h1 (repeat n text)]
   [:p "One day, this will be lovely music."]])

(rum/mount (label 30 "TRIGGERFISH! ") js/document.body)

(defn hello
  []
  (println "hello from client!"))
