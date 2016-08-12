(require '[figwheel-sidecar.repl-api :as ra]
         '[com.stuartsierra.component :as component])

(def figwheel-config
  {:figwheel-options {:css-dirs ["css"]}
   :build-ids ["client-dev" "server-dev"]
   :all-builds
   [{:id "client-dev"
     :source-paths ["src/triggerfish/client" "src/triggerfish/shared"]
     :figwheel {:websocket-host :js-client-host}
     :compiler {:main "triggerfish.client.core"
                :output-to "out/client_out/client.js"
                :output-dir "out/client_out"
                :source-map true}}
    {:id "server-dev"
     :source-paths ["src/triggerfish/server" "src/triggerfish/shared" "test/triggerfish_test/server"]
     :figwheel true
     :compiler {:main "triggerfish.server.core"
                :output-to "out/server_out/triggerfish_server_with_figwheel.js"
                :output-dir "out/server_out"
                :target :nodejs
                :source-map true}}
    {:id "client-prod"
     :source-paths ["src/triggerfish/client" "src/triggerfish/shared"]
     :compiler {:main "triggerfish.client.core"
                :output-to "out/client_prod/client.js"
                :output-dir "out/client_prod"
                :optimizations :advanced}}]})

(def sass-config
  {:executable-path "sass"
   :input-dir "sass"
   :output-dir "css"})

(defrecord Figwheel []
  component/Lifecycle
  (start [config]
    (ra/start-figwheel! config)
    config)
  (stop [config]
    (ra/stop-figwheel!)
    config))

(defrecord SassWatcher [executable-path input-dir output-dir]
  component/Lifecycle
  (start [config]
    (if (not (:sass-watcher-process config))
      (do
        (println "Figwheel: Starting SASS watch process")
        (assoc config :sass-watcher-process
               (.exec (Runtime/getRuntime)
                      (str executable-path " --watch " input-dir ":" output-dir))))
      config))
  (stop [config]
    (when-let [process (:sass-watcher-process config)]
      (println "Figwheel: Stopping SASS watch process")
      (.destroy process))
    config))

(def system
  (atom
   (component/system-map
    :figwheel (map->Figwheel figwheel-config)
    :sass (map->SassWatcher sass-config))))

(defn start []
  (swap! system component/start))

(defn stop []
  (swap! system component/stop))

(defn reload []
  (stop)
  (start))

(defn client-repl []
  (ra/cljs-repl "client-dev"))

(defn server-repl []
  (ra/cljs-repl "server-dev"))

(start)
;;To start Triggerfish from emacs:
;;Start the node process with "node out/server_out/triggerfish_server_with_figwheel.js" TODO: do this in script/figwheel.clj
;;Load browser at localhost:3000
;;in spacemacs you hit:
;;', s I' to cider jack in
;;', e b' to eval buffer
;;client-dev should be running by default, so it will wait for you to connect your browser (should work for any device on the LAN)
