(ns unknown-client.subs.forms.core
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/all-forms
 (fn [db _]
   (-> db :forms)))
