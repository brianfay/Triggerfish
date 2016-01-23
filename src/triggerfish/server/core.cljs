(ns triggerfish.server.core
  (:require
   [cljs.nodejs :as nodejs]
   [triggerfish.server.scsynth :as sc]))
   ;; [triggerfish.shared.objects :refer :all]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))
(defonce dgram (nodejs/require "dgram"))

(def app (express))

(. app (use (serve-static "./" #js {:index "index.html"})))

(def -main (fn []
    (doto (.createServer http #(app %1 %2))
        (.listen 3000))))

(set! *main-cli-fn* -main)



;; (call-scsynth "/d_load synthdefs/default.scsyndef")
;; (call-scsynth "/s_new default")
;; (call-scsynth "/notify 1")
;; (call-scsynth "/g_new 1000")


;; (defn remove-object
;;   "Removes an object from the patchwork"
;;   [id])

;;atom containing reference to objects (in no particular order) and their connections
;; (def patch (atom))

;;ordered list of objects, obtained by doing a topological sort on the dependency graph of the patchwork.
;;?Do I need this? probably need it client-side but maybe not server-side. then again it makes sense to calc once on server, send to client, idunno
;;could maybe calc everything on client, send to server, update and then send back new state

;;need diff algorithm for dependency graph - return additions, deletions, and movements
;; (def flat-patch (atom))

;; (defn get-free-id
;;   "Retrieves a node id that is not currently being used.")

;; (defn diff-sorted
;;   [old new]
;;   (let [[old new both] (diff old new)]))
