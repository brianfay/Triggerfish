(ns ^:figwheel-always triggerfish.server.core
  (:require
   [cljs.nodejs :as nodejs]
   [triggerfish.server.patch]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.express :as sente-express]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce express-ws (nodejs/require "express-ws"))
(defonce ws (nodejs/require "ws"))
(defonce serve-static (nodejs/require "serve-static"))
(defonce http (nodejs/require "http"))

(let [packer :edn
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente-express/make-express-channel-socket-server! {:packer packer})]
  (defonce ajax-post                ajax-post-fn)
  (defonce ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (defonce ch-chsk                  ch-recv)
  (defonce chsk-send                send-fn)
  (defonce connected-uids           connected-uids))

(defn- handle
  [express-app]
  (doto express-app
    (.use (.static express "./"))
    (.ws "/chsk"
         (fn [ws req next]
           (ajax-get-or-ws-handshake req nil nil
                                     :websocket? true
                                     :websocket ws)))))

(defn start-server!
  []
  (let [express-app (express)
        express-ws-server (express-ws express-app)]
    (handle express-app)
    (.listen express-app 3000)))

;; ;; (. app (use (serve-static "./" #js {:index "index.html"})))

(def -main (fn []
    ;; (doto (.createServer http #(app %1 %2))
             ;;   (.listen 3000))
             (start-server!)))
             ;; (start-server!)))

(set! *main-cli-fn* -main)
