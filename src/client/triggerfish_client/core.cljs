(ns triggerfish-client.core
  (:require [rum.core :as rum]))

(rum/defc label [n text]
  [:div
   [:h1 (repeat n text)]
   [:p "One day, this will be lovely music."]])

(rum/mount (label 2 "TRIGGERFISH! ") js/document.body)
