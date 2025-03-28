(ns schmoho.dasudopit.client.panels.volcano-viewer.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]))

(rf/reg-sub
 :forms/volcano-viewer
 :<- [::forms/all-forms]
 (fn [forms]
   (:volcano-viewer forms)))

(rf/reg-sub
 :forms.volcano/go-term-selection
 :<- [:forms/volcano-viewer]
 (fn [form [_ left-right]]
   (-> form (get left-right) :go-filter set)))
