(ns schmoho.dasudopit.client.common.http
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def base-api "http://localhost:3001/api")

(rf/reg-event-fx
 ::http-get
 (fn-traced
  [{:keys [db]} [_ path]]
  {:db db
   :http-xhrio
   {:method          :get
    :uri             (str base-api "/"
                          (str/join "/" (map name path)))
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      (into [::http-get-success path])
    :on-failure      (into [::http-failure path])}}))

(rf/reg-event-fx
 ::http-get-2
 (fn-traced
  [{:keys [db]} [_ path]]
  {:db db
   :http-xhrio
   {:method          :get
    :uri             (str base-api "/"
                          (str/join "/" (map name path)))
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      (into [::http-get-success-2 path])
    :on-failure      (into [::http-failure path])}}))

(rf/reg-event-db
 ::http-get-success
 (fn-traced
  [db [_ path response]]
  (-> (assoc-in db path response)
      (update :already-executed-queries conj path))))

(rf/reg-event-db
 ::http-get-success-2
 (fn-traced
  [db [_ path response]]
  (-> (update-in db path #(merge % response))
      (update :already-executed-queries conj path))))

(rf/reg-event-fx
 ::http-failure
 (fn-traced
  [{:keys [db]} response]
  (js/alert response)))
