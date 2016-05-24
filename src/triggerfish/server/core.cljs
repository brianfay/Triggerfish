(ns ^:figwheel-always triggerfish.server.core
  (:require
   [triggerfish-test.server.manual] ;;just so we can play with it at the repl
   [triggerfish.server.patch :as p]
   [triggerfish.server.events :as events]
   [cljs.nodejs        :as nodejs]
   [clojure.string     :as str]
   [cljs.core.async    :as async  :refer (<! >! put! chan)]
   [taoensso.encore    :as encore :refer ()]
   [taoensso.timbre    :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [taoensso.sente     :as sente]
   [taoensso.sente.server-adapters.express :as sente-express]))

(enable-console-print!)

(def http (nodejs/require "http"))
(def express (nodejs/require "express"))
(def express-ws (nodejs/require "express-ws"))
(def ws (nodejs/require "ws"))
(def cookie-parser (nodejs/require "cookie-parser"))
(def body-parser (nodejs/require "body-parser"))
(def csurf (nodejs/require "csurf"))
(def session (nodejs/require "express-session"))

(let [;; Serializtion format, must use same val for client + server:
      packer :edn ; Default packer, a good choice in most cases
      user-id-fn (fn [ring-req] (:client-id ring-req))
      ;; (sente-transit/get-flexi-packer :edn) ; Experimental, needs Transit dep
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente-express/make-express-channel-socket-server! {:packer packer
                                                          :user-id-fn user-id-fn})]
  (def ajax-post                ajax-post-fn)
  (def ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                  ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!               send-fn) ; ChannelSocket's send API fn
  (def connected-uids           connected-uids) ; Watchable, read-only atom
  )

(defn get-csrf-token
  "Sente uses 'csrf-token' in it's request but csurf usually uses
  '_csrf'.  This extends what csurf looks for."
  [req]
  (let [body (.-body req)
        query (.-query req)
        headers (.-headers req)]
    (or
     (aget body "_csrf") (aget body "csrf-token")
     (aget query "_csrf") (aget query "csrf-token")
     (aget headers "csrf-token") (aget headers "xsrf-token")
     (aget headers "x-csrf-token") (aget headers "x-xsrf-token"))))

(defn routes [express-app]
  (doto express-app
    (.use (.static express "./")) ;;let's us get index.html
    (.ws "/chsk"
         (fn [ws req next]
           (ajax-get-or-ws-handshake req nil nil
                                     {:websocket? true
                                      :websocket ws})))
    (.get "/chsk" ajax-get-or-ws-handshake)
    (.post "/chsk" ajax-post)
    (.use (fn [req res next]
            (warnf "Unhandled request: %s" (.-originalUrl req))
            (next)))))

(defn wrap-defaults [express-app routes]
  (let [cookie-secret "the shiz"]
    (doto express-app
      (.use (fn [req res next]
              (tracef "Request: %s" (.-originalUrl req))
              (next)))
      (.use (session
             #js {:secret cookie-secret
                  :resave true
                  :cookie {}
                  :store (.MemoryStore session)
                  :saveUninitialized true}))
      (.use (.urlencoded body-parser
                         #js {:extended false}))
      (.use (cookie-parser cookie-secret))
      (.use (csurf
             #js {:cookie false
                  :value get-csrf-token}))
      (routes))))

(defn main-ring-handler [express-app]
  (wrap-defaults express-app routes))

(defn start-selected-web-server! [ring-handler port]
  (println "Starting express...")
  (let [express-app (express)
        express-ws-server (express-ws express-app)]

    (ring-handler express-app)

    (let [http-server (.listen express-app port)]
      {:express-app express-app
       :ws-server express-ws-server
       :http-server http-server
       :stop-fn #(.close http-server)
       :port port})))

;;;; Sente event router (our `event-msg-handler` loop)
(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk events/event-msg-handler)))

;;;; Init stuff
(defonce web-server_ (atom nil)) ; {:server _ :port _ :stop-fn (fn [])}
(defn  stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-selected-web-server! (var main-ring-handler) (or port 3000))
        uri (str "http://localhost:" port "/")]
    (infof "Web server is running at `%s`" uri)
    (reset! web-server_ server-map)))

(defn stop!  []  (stop-router!)  (stop-web-server!))
(defn start! [] (start-router!) (start-web-server!))
;; (defonce _start-once (start!))

(defn -main [& _]
  (start!))

(set! *main-cli-fn* -main) ;; this is required

(start-router!)

;;add a watch to the patch atom
;; (defonce patch-watch (add-watch p/patch :notify-patch
;;                                 (fn [key atom old-state new-state]
;;                                   (doseq [uid (:any @connected-uids)]
;;                                     (chsk-send! uid [:patch/recv {:patch (p/get-patch-map)}])))))
