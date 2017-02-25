(ns ^:figwheel-always triggerfish.server.core
  (:require
   [triggerfish-test.server.manual] ;;just so we can play with it at the repl
   [triggerfish.server.object.object-definitions] ;;requiring so these object definitions will load
   [triggerfish.server.sente :as sente]))

(enable-console-print!)

(defn -main [& _]
  (sente/start!))

(set! *main-cli-fn* -main)
