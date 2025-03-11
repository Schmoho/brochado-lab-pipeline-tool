(ns unknown-client.subs.forms.msa
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/msa
 :<- [:forms/all-forms]
 (fn [forms]
   (:msa forms)))

