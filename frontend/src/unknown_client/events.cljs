(ns unknown-client.events
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [day8.re-frame.http-fx]
   [ajax.core :as ajax] 
   [unknown-client.tours :as tours]
   [re-com.core :as re-com]
   [unknown-client.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

;; @re-frame.db/app-db

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

(re-frame/reg-event-fx
 ::navigate
 (fn-traced [_ [_ handler]]
            {:navigate       handler
             :get-route-data handler}))

(re-frame/reg-event-fx
 ::set-active-route
 (fn-traced [{:keys [db]} [_ route]]
            {:db (assoc db
                        :active-route route
                        :active-panel (:handler route))}))

(re-frame/reg-event-db
 ::set-form-data
 (fn-traced [db [_ form & keys]]
            (prn (concat [form :form]
                                 (butlast keys)))
            (assoc-in db (concat [form :form]
                                 (butlast keys))
                      (last keys))))

(re-frame/reg-event-db
 ::toggle-form-bool
 (fn-traced [db [_ form & keys]]
            (update-in db (concat [form :form]
                                  keys)
                       not)))

(re-frame/reg-event-fx
 ::start-a-tour
 (fn-traced
  [{:keys [db]} [_ tour-key]]
  (let [tour (re-com/make-tour (tours/tour-lookup tour-key))]
    {:db db
     :start-tour tour})))

(def base-api "http://localhost:3001")

(re-frame/reg-event-fx
 ::start-taxonomic-comparison!
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :post
    :uri             (str base-api "/taxonomic-comparison")
    :params          (-> db :taxonomic-comparison :form
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
    :on-success      [::start-taxonomic-comparison-success]
    :on-failure      [::http-failure]}}))

(re-frame/reg-event-fx
 ::get-taxonomic-comparison-results
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :get
    :uri             (str base-api "/taxonomic-comparison-results")
    :timeout         10000
    :format          (ajax/transit-request-format)
    :response-format (ajax/transit-response-format {:keywords? true})
    :on-success      [::get-taxonomic-comparison-results-success]
    :on-failure      [::http-failure]}}))

(re-frame/reg-event-fx
 ::http-failure
 (fn-traced
  [{:keys [db]} [_ response]]
  (js/alert response)))

(re-frame/reg-event-fx
 ::start-taxonomic-comparison-success
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db       db
   :navigate :taxonomic-comparison-results}))

(re-frame/reg-event-fx
 ::get-taxonomic-comparison-results-success
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db       (assoc-in db [:taxonomic-comparison :results] (zipmap
                                                            (map :pipeline/uuid
                                                                 (:results response))
                                                            (:results response)))
   :navigate :taxonomic-comparison-results}))

(re-frame/reg-event-fx
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
    :on-success      (into [::http-success path])
    :on-failure      (into [::http-failure path])}}))

(re-frame/reg-event-db
 ::http-success
 (fn-traced
  [db [_ path response]]
  (reduce
   (fn [acc datum]
     (assoc-in acc
               (concat path [(:id datum)])
               datum))
   db
   (:results response))))
