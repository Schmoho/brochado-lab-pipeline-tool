(ns schmoho.dasudopit.client.common.http
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
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
    :on-request      [::register-running-query path]
    :on-success      [::http-get-success path]
    :on-failure      [::http-failure path]}}))

(rf/reg-event-db
 ::http-get-success
 (fn-traced
  [db [_ path response]]
  (-> db
      (assoc-in path response)
      (assoc-in [:queries path] :done))))

(rf/reg-event-fx
 ::http-post
 (fn-traced
  [{:keys [db]} [_ path {:keys [params success-event]}]]
  (let [api-segment (->> (map name path)
                         (str/join "/")
                         (str "/"))]
    {:db db
     :http-xhrio
     {:method          :post
      :uri             (str base-api api-segment)
      :params          (or params {})
      :timeout         10000
      :format          (ajax/transit-request-format)
      :response-format (ajax/transit-response-format {:keywords? true})
      :on-request      [::register-running-query path]
      :on-success      (or success-event [::http-post-success path])
      :on-failure      [::http-failure path]}})))


(rf/reg-event-db
 ::http-post-success
 (fn-traced
  [db [_ path response]]
  (-> db
      (assoc-in path response)
      (assoc-in [:queries path] :done))))

(rf/reg-event-fx
 ::http-delete
 (fn-traced
  [{:keys [db]} [_ path]]
  {:db db
   :http-xhrio
   {:method          :delete
    :uri             (str base-api "/"
                          (str/join "/" (map name path)))
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-request      [::register-running-query path]
    :on-success      [::http-delete-success path]
    :on-failure      [::http-failure path]}}))


(rf/reg-event-db
 ::http-delete-success
 (fn-traced
  [db [_ path]]
  (-> db
      (update-in (butlast path) #(dissoc % (last path)))
      (update-in [:queries path] dissoc))))

(rf/reg-event-db
 ::http-failure
 (fn-traced
  [db [_ path response]]
  (-> db
      (assoc-in [:failures path] response)
      (assoc-in [:queries path] :failure))))


(rf/reg-event-db
 ::register-running-query
 (fn-traced
  [db [_ path]]
  (assoc-in db [:queries path] :running)))

(rf/reg-sub
 ::queries
 (fn [db]
   (:queries db)))
