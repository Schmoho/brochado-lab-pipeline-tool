(ns unknown-client.subs.forms.docking
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/docking
 :<- [:forms/all-forms]
 (fn [forms]
   (:docking forms)))

(rf/reg-sub
 :docking/taxon-model
 :<- [:forms/docking]
 (fn [form]
   (vec (:taxon-model form))))
