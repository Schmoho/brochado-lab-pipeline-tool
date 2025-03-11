(ns unknown-client.subs.routing
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::active-route
 (fn [db _]
   (:active-route db)))

(rf/reg-sub
 ::active-panel
 :<- [::active-route]
 (fn [active-route]
   (:handler active-route)))

(rf/reg-sub
 ::active-route-params
 :<- [::active-route]
 (fn [active-route]
   (:route-params active-route)))
