(ns schmoho.dasudopit.client.panels.data.protein
  (:require
   [re-com.core :as re-com :refer [at] :rename {h-box h}]
   [schmoho.dasudopit.client.routing :as routing]))

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
