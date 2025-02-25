(ns unknown-client.events
  (:require
   [re-frame.core :as re-frame]
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

(def base-api "https://localhost:3001")
