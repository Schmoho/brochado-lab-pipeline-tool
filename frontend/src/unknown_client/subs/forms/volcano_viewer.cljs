(ns unknown-client.subs.forms.volcano-viewer
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :forms/volcano-viewer
 :<- [:forms/all-forms]
 (fn [forms]
   (:volcano-viewer forms)))
