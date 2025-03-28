(ns schmoho.dasudopit.client.panels.pipelines.msa.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]))

(rf/reg-sub
 :forms/msa
 :<- [::forms/all-forms]
 (fn [forms]
   (:msa forms)))

