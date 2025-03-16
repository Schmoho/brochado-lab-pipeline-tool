(ns schmoho.dasudopit.client.common.forms
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

;; === Events ===

(rf/reg-event-db
 ::set-form-data
 (fn-traced [db [_ form & keys]]
            (assoc-in db (concat [:forms form]
                                 (butlast keys))
                      (last keys))))

(rf/reg-event-db
 ::update-form-data
 (fn-traced [db [_ form & keys]]
            (update-in db (concat [:forms form]
                                 (butlast keys))
                       (last keys))))

(rf/reg-event-db
 ::toggle-form-bool
 (fn-traced [db [_ form & keys]]
            (update-in db (concat [:forms form]
                                  keys)
                       not)))

;; === Subs ===

(rf/reg-sub
 :forms/all-forms
 (fn [db _]
   (-> db :forms)))
