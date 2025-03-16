(ns schmoho.dasudopit.client.panels.pipelines.msa.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/msa
 :<- [:forms/all-forms]
 (fn [forms]
   (:msa forms)))

