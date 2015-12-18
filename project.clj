(defproject triggerfish "0.1.0-SNAPSHOT"
  :description "Serves a web front-end for supercollider, intended for real-time music performance"
  :url "http://github.com/yottasecond/Triggerfish"
  :license {:name "The MIT License (MIT)"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [rum "0.5.0"]]

  :plugins [[lein-figwheel "0.5.0-2"]
            [lein-npm "0.6.1"]]

  :npm {:dependencies [[express "4.13.3"]
                       [serve-static "1.10.0"]
                       [ws "0.8.0"]
                       [osc-min "0.2.0"]]}

  :clean-targets ^{:protect false} ["target"]

  :cljsbuild {
    :builds
        [{:id "client-dev"
            :source-paths ["src/client"]
            :figwheel true
          :compiler {:main "client.core"
                     :output-to "out/client_out/client.js"
                     :output-dir "out/client_out"
                     :source-map true}}
        {:id "server-dev"
            :source-paths ["src/server"]
            :figwheel true
            :compiler {:main "server.core"
                    :output-to "out/server_out/triggerfish_server_with_figwheel.js"
                    :output-dir "out/server_out"
                    :target :nodejs
                    :optimizations :none
                    :source-map true}}]}
  :figwheel {
             :server-port 4000
             :css-dirs ["css"]})
