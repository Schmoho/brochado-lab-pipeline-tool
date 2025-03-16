(ns schmoho.dasudopit.client.panels.volcano-viewer.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/volcano-viewer
 :<- [:forms/all-forms]
 (fn [forms]
   (:volcano-viewer forms)))

(rf/reg-sub
 :forms.volcano/go-term-selection
 :<- [:forms/volcano-viewer]
 (fn [form]
   (set (:go-filter form))))
