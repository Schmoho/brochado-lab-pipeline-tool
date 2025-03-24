(ns schmoho.dasudopit.client.panels.volcano-viewer.events
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [schmoho.dasudopit.client.http :as http]))

(rf/reg-event-fx
 ::selection
 (fn-traced
  [{:keys [db]} [_ left-right selection]]
  (let [volcano-taxon    (-> db :data :volcano (get selection) :meta :taxon)
        volcano-proteome (-> db :data (get volcano-taxon) :proteome)
        volcano-data     (-> db :data :volcano (get selection) :table)]
    (when-not volcano-data
      (rf/dispatch [::http/http-get [:data :volcano selection]]))
    (when-not volcano-proteome
      (rf/dispatch [::http/http-get [:data :taxon volcano-taxon :proteome]]))
    {:db (assoc-in db [:forms :volcano-viewer left-right :volcano] selection)})))
