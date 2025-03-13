(ns unknown-client.events.vega
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-fx
 ::register-listener-fx
 (fn [[view listener-opts]]
   (.addSignalListener
    ^View view
    (:signal-name listener-opts)
    (:listener-fn listener-opts))))

(rf/reg-event-fx
 ::register-signal-listener
 (fn-traced
  [{:keys [db]} [_ view listener]]
  {:db db
   ::register-listener-fx [view listener]}))

(rf/reg-event-db
 ::selection
 (fn-traced
  [db [_ selection]]
  (assoc-in db [:forms :volcano-viewer :volcano-1-selection] selection)))
