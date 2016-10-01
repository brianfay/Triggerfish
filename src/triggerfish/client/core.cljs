(ns triggerfish.client.core
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch
                                   dispatch-sync
                                   subscribe]]
            [triggerfish.client.sente-events :as sente-events]
            [triggerfish.client.handlers]
            [triggerfish.client.subs]
            [triggerfish.client.views :as views]))

(enable-console-print!)

(reagent/render [views/app]
                (js/document.getElementById "app"))

;;;; Init stuff
(defn start! []
  (dispatch-sync [:initialize])
  (sente-events/start-router!))

(defonce _start-once (start!))
