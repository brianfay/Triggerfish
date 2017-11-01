(ns triggerfish.server.hack
  (:require [cljs.nodejs :as nodejs]
            [octet.core :as buf]))

(defonce fs (nodejs/require "fs"))

(def SCgf 0x53436766) ;;int32 representing the characters SCgf

(defn spit [filename content]
  (.writeFile fs filename content (fn [e] (println "spat to file: " e))))

(defn synthdef-header []
  (let [header-spec (buf/spec buf/int32 buf/int32)
        buffer (buf/allocate (buf/size header-spec))]
    (buf/write! buffer [SCgf 2] header-spec)
    buffer))


(comment
  (let [wstream (.createWriteStream fs "testYo.raw")
        buf (js/Uint8Array. (.-buffer (synthdef-header)))]
    (.write wstream buf)
    (.end wstream))

  (println (.-byteLength (.-buffer (synthdef-header))))
  (js/Uint8Array. (.-buffer (synthdef-header)))
  )
