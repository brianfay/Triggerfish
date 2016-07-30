(ns triggerfish.server.midi
  (:require
   [cljs.nodejs :as nodejs]))

(def m (js/eval "midi = require('midi')"));;yeah I know, js/eval looks pretty bad...
(def input (js/eval "new midi.input()"))  ;;but please explain to me how else I'm supposed to do this?
