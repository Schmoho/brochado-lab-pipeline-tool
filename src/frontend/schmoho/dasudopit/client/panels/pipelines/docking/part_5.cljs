(ns schmoho.dasudopit.client.panels.pipelines.docking.part-5
  (:require
   [re-com.core :as com :rename {v-box v}]
   [schmoho.components.forms :as forms]))


(defn part-5
  [proteins]
  [v
   :children
   [[:h4 {:style {:color "darkred"}} "Dummy button for illustration"]
    [:div [forms/action-button
           :label "Download"]]]])
