(ns schmoho.dasudopit.client.panels.pipelines.docking.choose-binding-sites
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.css.forms :as css]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs :as subs]))

(defn choose-binding-sites-form
  []
  ;; (let [form-valid?        @(rf/subscribe [:forms.docking.choose-binding-sites/valid?])]
  ;;   [v
  ;;    :children
  ;;    [(when-not form-valid?
  ;;       [:span "Please choose a protein for each taxon and press the button to get the structures."])
  ;;     [h
  ;;      :min-height "300px"
  ;;      :gap "30px"
  ;;      :children
  ;;      (into [] proteome-searchers)]
  ;;     [get-structures-button]]])
  #_[get-structures-button]
  [h
   :children
   [[:button
     {:on-click #(rf/dispatch
                  [::http/http-post [:pipelines :docking :run]
                   {:params @(rf/subscribe [::subs/form])}])}
     "Download docking simulation package"]
    [:button "Upload docking results"]]])
