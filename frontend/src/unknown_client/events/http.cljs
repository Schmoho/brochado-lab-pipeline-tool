(ns unknown-client.events.http
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def base-api "http://localhost:3001")

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
  (-> (update-in db (butlast path) #(merge % response))
      (update :already-executed-queries conj path))))

(rf/reg-event-db
 ::http-get-success
 (fn-traced
  [db [_ path response]]
  (-> (update-in db (butlast path) #(merge % response))
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

(rf/reg-event-fx
 ::start-msa!
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :post
    :uri             (str base-api "/pipelines/msa")
    :params          (-> db :forms :msa
                         (update-in [:params.uniprot/protein :protein-ids]
                                    #(-> %
                                         (str/replace #"[,;]" " ")
                                         (str/split #"\s+")
                                         set))
                         (update-in [:params.uniprot/protein :gene-names]
                                    #(-> %
                                         (str/replace #"[,;]" " ")
                                         (str/split #"\s+")
                                         set)))
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      [::start-msa-success]
    :on-failure      [::http-failure]}}))

(rf/reg-event-fx
 ::get-msa-results
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :get
    :uri             (str base-api "/results/msa")
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      [::get-msa-results-success]
    :on-failure      [::http-failure]}}))

(rf/reg-event-fx
 ::start-msa-success
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db       db
   :navigate :routing.results/msa}))

(rf/reg-event-fx
 ::get-msa-results-success
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db       (assoc-in db [:results :msa] (zipmap
                                           (map :pipeline/uuid
                                                (:results response))
                                           (:results response)))
   :navigate :routing.results/msa}))


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
