(ns triggerfish.server.core
  (:require
   [cljs.nodejs :as nodejs]
   [triggerfish.server.patch]))

(nodejs/enable-util-print!)


(defonce express (nodejs/require "express"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))

(def app (express))

(. app (use (serve-static "./" #js {:index "index.html"})))

(def -main (fn []
    (doto (.createServer http #(app %1 %2))
        (.listen 3000))))

(set! *main-cli-fn* -main)
