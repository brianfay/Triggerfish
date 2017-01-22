(defproject triggerfish "0.1.0-SNAPSHOT"
  :description "Serves a web front-end for supercollider, intended for real-time music performance"
  :url "http://github.com/yottasecond/Triggerfish"
  :license {:name "The MIT License (MIT)"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/core.async "0.2.374"]
                 [cljsjs/hammer "2.0.4-5"]
                 [com.stuartsierra/dependency "0.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/sente "1.8.0-beta1"]
                 [com.taoensso/timbre "4.2.1"]
                 ;;these should maybe be in a :dev :dependencies?
                 [binaryage/devtools "0.8.3"]
                 [figwheel-sidecar "0.5.4-7"]
                 [com.cemerick/piggieback "0.2.1"]
                 [reagent "0.6.0-rc"]
                 [re-frame "0.8.0"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.2"]]

  :npm {:dependencies [[express "4.13.3"]
                       [serve-static "1.10.0"]
                       [body-parser "1.14.1"]
                       [cookie-parser "1.4.0"]
                       [express-session "1.11.3"]
                       [csurf "1.8.3"]
                       [express-ws "1.0.0-rc.2"]
                       [midi "0.9.5"]
                       [nexusui "lsu-emdm/nexusUI"]
                       [ws "0.8.0"]
                       [osc-min "0.2.0"]]}

  :clean-targets ^{:protect false} ["target"]

  :figwheel {:css-dirs ["css"]}

  :cljsbuild {
    :builds
        [{:id "client-dev"
          :figwheel {:websocket-host :js-client-host} ;;this took so long to figure out, I dunno why
          :source-paths ["src/triggerfish/client" "src/triggerfish/shared"]
          :compiler {:main "triggerfish.client.core"
                     :output-to "out/client_out/client.js"
                     :output-dir "out/client_out"
                     :preloads [devtools.preload]
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
                     :optimizations :advanced}}]}
)
