(ns schmoho.dasudopit.client.panels.volcano-viewer.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [schmoho.dasudopit.client.common.http :as http]))

(rf/reg-event-fx
 ::selection
 (fn-traced
  [{:keys [db]} [_ left-right selection]]
  (let [volcano-taxon    (-> db :data :input :volcano (get selection) :meta :taxon)
        volcano-proteome (-> db :data :raw :proteome (get volcano-taxon))]
    (when-not volcano-proteome
      (rf/dispatch [::http/http-get [:data :raw :proteome volcano-taxon]]))
    {:db (assoc-in db [:forms :volcano-viewer left-right :volcano] selection)})))
