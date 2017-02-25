(ns triggerfish.server.sente
  (:require
   [triggerfish.server.object.core :as obj]
   [triggerfish.server.midi :as midi]
   [triggerfish.server.sente.events :as events]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.express :as sente-express]
   [cljs.nodejs        :as nodejs]))

(defonce express (nodejs/require "express"))
(defonce express-ws (nodejs/require "express-ws"))
(defonce ws (nodejs/require "ws"))
(defonce cookie-parser (nodejs/require "cookie-parser"))
(defonce csurf (nodejs/require "csurf"))
(defonce session (nodejs/require "express-session"))

(let [user-id-fn (fn [ring-req] (:client-id ring-req))
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]} (sente-express/make-express-channel-socket-server! {:packer :edn
                                                                                   :user-id-fn user-id-fn})]
  (def ajax-post                ajax-post-fn)
  (def ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                  ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!               send-fn) ; ChannelSocket's send API fn
  (def uids           connected-uids))

(defn routes [express-app]
  (doto express-app
    (.use (.static express "./")) ;;lets us get index.html
    (.ws "/chsk"
         (fn [ws req next]
           (ajax-get-or-ws-handshake req nil nil
                                     {:websocket? true
                                      :websocket ws})))
    (.get "/chsk" ajax-get-or-ws-handshake)
    (.post "/chsk" ajax-post)
    (.use (fn [req res next]
            (println "Unhandled request: " (.-originalUrl req))
            (next)))))

(defn wrap-defaults [express-app routes]
  (let [cookie-secret "Secret Cherie"]
    (doto express-app ;;this stuff is mostly copied from theasp on github, don't particularly care about cookie secrets and csrf for this application but I guess it doesn't hurt to have
      (.use (session
             #js {:secret cookie-secret
                  :resave true
                  :cookie {}
                  :store (.MemoryStore session)
                  :saveUninitialized true}))
      (.use (csurf
             #js {:cookie false}))
      (routes))))

(defn main-ring-handler [express-app]
  (wrap-defaults express-app routes))

(defn start-selected-web-server! [ring-handler port]
  (let [express-app (express)
        express-ws-server (express-ws express-app)]
    (ring-handler express-app)
    (let [http-server (.listen express-app port)]
      {:express-app express-app
       :ws-server express-ws-server
       :http-server http-server
       :stop-fn #(.close http-server)
       :port port})))

;;;; Init stuff
(defonce web-server_ (atom nil))
(defn stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))
(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-selected-web-server! (var main-ring-handler) (or port 3000))
        uri (str "http://localhost:" port "/")]
    (println "Web server is running at " uri)
    (reset! web-server_ server-map)))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk events/event-msg-handler)))

(defn stop!  [] (stop-router!)  (stop-web-server!))
(defn start! [] (start-router!) (start-web-server!))

(defonce _start-once (start-router!))

(defonce fiddler-on-the-roof ;;watch for most recently fiddled midi controls, send to the client
  (add-watch midi/recently-fiddled :fiddled
             (fn [key ref old-state new-state]
               (doseq [uid (:any @uids)]
                 (chsk-send! uid [:fiddled/recv new-state])))))

(defonce obj-defs ;;watch for new object-defs
  (add-watch obj/object-registry :obj-defs
             (fn [key ref old-state new-state]
               (doseq [uid (:any @uids)]
                 (chsk-send! uid [:obj-defs/recv (obj/get-public-obj-defs)])))))
