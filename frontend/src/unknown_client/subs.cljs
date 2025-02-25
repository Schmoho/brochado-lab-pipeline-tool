(ns unknown-client.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-route
 (fn [db _]
   (:active-route db)))

(re-frame/reg-sub
 ::active-panel
 :<- [::active-route]
 (fn [active-route]
   (:handler active-route)))

(re-frame/reg-sub
 ::active-route-params
 :<- [::active-route]
 (fn [active-route]
   (:route-params active-route)))
