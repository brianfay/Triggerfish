(ns triggerfish.client.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
 :objects
 (fn
   [db _]
   (reaction (:objects @db))))

(register-sub
 :positions
 (fn
   [db _]
   (reaction (:positions @db))))

(register-sub
 :minimized
 (fn
   [db _]
   (reaction (:minimized @db))))

(register-sub
 :connections
 (fn
   [db _]
   (reaction (:connections @db))))

(register-sub
 :selected-create-object
 (fn
   [db _]
   (reaction (:selected-create-object @db))))

(register-sub
 :selected-outlet
 (fn
   [db _]
   (reaction (:selected-outlet @db))))

(register-sub
 :mode
 (fn
   [db _]
   (reaction (:mode @db))))

(register-sub
 :patch-size
 (fn
   [db _]
   (reaction (:patch-size @db))))

(register-sub
 :sidebar-open
 (fn
   [db _]
    (reaction (:sidebar-open @db))))
