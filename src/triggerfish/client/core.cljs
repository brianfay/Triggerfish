(ns triggerfish.client.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [cljs.core.async :as async :refer [<! >! put! chan]]
            [taoensso.sente :as sente :refer [cb-success?]]))

(let [chsk-type :auto
      packer :edn
      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk"
       {:type chsk-type
        :packer packer})]
  (defonce chsk chsk)
  (defonce ch-chsk ch-recv)
  (defonce chsk-send! send-fn)
  (defonce chsk-state state))

(when-let [target (.getElementById js/document "btn1")]
  (.addEventListener target "click"
                     (fn [ev]
                       (chsk-send! [:chsk {:had-a-callback? "nope"}]))))

(defn example
  []
  [:div
   "Hello Brian, I am Triggerfish! It's so nice to meet you!"
   [:button {:id "btn1"}"CLICK ME I TALK TO THE SERVER"]])

(reagent/render [example]
                (js/document.getElementById "app"))
