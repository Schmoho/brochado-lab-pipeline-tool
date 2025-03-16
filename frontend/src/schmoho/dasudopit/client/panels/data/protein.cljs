(ns schmoho.dasudopit.client.panels.data.protein
  (:require
   [schmoho.dasudopit.client.routing :as routing]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn single-protein-panel
  []
  [h
   :width "80%"
   :justify :between
   :children
   [[:div "Hi"]]])

(defn single-protein-header []
  [re-com/title
   :src   (at)
   :label "Protein View"
   :level :level1])

(defmethod routing/header :protein-entry [] [single-protein-header])
(defmethod routing/panels :protein-entry [] [single-protein-panel])
