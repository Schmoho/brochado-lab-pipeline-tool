(ns unknown-client.events
  (:require
   [re-frame.core :as re-frame]
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
            {:navigate handler}))

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

(def base-api "https://localhost:3001")

(re-frame/reg-event-fx
 ::start-taxonomic-comparison!
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio
   {:method          :post
    :uri             (str base-api "/taxonomic-comparison")
    :params          (-> db :taxonomic-comparison :form)
    :timeout         10000
    :on-success      [::post-taxonomic-comparison-success]
    :on-failure      [::post-taxonomic-comparison-failure]}}))
