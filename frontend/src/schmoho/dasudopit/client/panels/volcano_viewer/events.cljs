(ns schmoho.dasudopit.client.panels.volcano-viewer.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-db
 ::selection
 (fn-traced
  [db [_ selection]]
  (assoc-in db [:forms :volcano-viewer :volcano-1-selection] selection)))
