(ns unknown-client.events.routing
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-fx
 ::navigate
 (fn-traced [_ [_ handler]]
            {:navigate handler}))

(rf/reg-event-fx
 ::set-active-route
 (fn-traced [{:keys [db]} [_ route]]
            {:db             (assoc db
                                    :active-route route
                                    :active-panel (:handler route))
             :get-route-data (:handler route)}))
