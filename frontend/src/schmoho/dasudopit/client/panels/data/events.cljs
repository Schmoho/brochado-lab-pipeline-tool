(ns schmoho.dasudopit.client.panels.data.events
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def base-api "http://localhost:3001/api")

(rf/reg-event-fx
 ::post-volcano
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :post
    :uri             (str base-api "/data/upload/volcano")
    :params          (-> db :forms :upload/volcano)
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      [::post-volcano-success]
    :on-failure      [::http-failure]}}))


(rf/reg-event-db
 ::post-volcano-success
 (fn-traced
  [db [_ response]]
  (-> db (update-in [:data :input :volcano] #(merge % response)))))
