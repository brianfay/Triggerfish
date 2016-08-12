(ns triggerfish.client.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :objects
 (fn
   [db _]
   (:objects db)))

(reg-sub
 :positions
 (fn
   [db _]
   (:positions db)))

(reg-sub
 :minimized
 (fn
   [db _]
   (:minimized db)))

(reg-sub
 :connections
 (fn
   [db _]
   (:connections db)))

(reg-sub
 :selected-create-object
 (fn
   [db _]
   (:selected-create-object db)))

(reg-sub
 :selected-control-object
 (fn
   [db _]
   (:selected-control-object db)))

(reg-sub
 :selected-outlet
 (fn
   [db _]
   (:selected-outlet db)))

(reg-sub
 :mode
 (fn
   [db _]
   (:mode db)))

(reg-sub
 :patch-size
 (fn
   [db _]
   (:patch-size db)))

(reg-sub
 :toolbar-hidden
 (fn
   [db _]
   (:toolbar-hidden db)))
