(ns schmoho.dasudopit.client.forms
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
 ::all-forms
 (fn [db _]
   (-> db :forms)))

(rf/reg-sub
 :forms/by-path
 :<- [::all-forms]
 (fn [forms [_ & path]]
   (get-in forms path)))

;; === Utils ===

(defn model
  [form-model k]
  (let [path (get form-model k)]
    (rf/subscribe (into [:forms/by-path] path))))

(defn setter
  [form-model k]
  (let [path (get form-model k)]
    #(rf/dispatch (conj (into [::set-form-data] path)
                        %))))
