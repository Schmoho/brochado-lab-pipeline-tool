(ns schmoho.dasudopit.client.panels.data.upload
  (:require
   [re-com.core :as com :rename {v-box v}]
   [schmoho.components.structure :as structure :refer [card]]
   [schmoho.dasudopit.client.panels.data.upload.ligand
    :refer [provision-ligand-form]]
   [schmoho.dasudopit.client.panels.data.upload.structure
    :refer [upload-structure-form]]
   [schmoho.dasudopit.client.panels.data.upload.taxon
    :refer [provision-taxon-form]]
   [schmoho.dasudopit.client.panels.data.upload.volcano
    :refer [upload-volcano-form]]
   [schmoho.dasudopit.client.routing :as routing]))

(defn upload-data-panel
  []
  [v
   :gap "20px"
   :children
   [[upload-structure-form]
    #_[upload-volcano-form]
    #_[card
     "Provision Ligand via Pubchem"
     [provision-ligand-form]]
    #_[card
     "Provision Organism via Uniprot"
     [provision-taxon-form]]]])

(defmethod routing/panels :routing.data/upload [] [upload-data-panel])
(defmethod routing/header :routing.data/upload []
  [structure/header :label "Upload Data"])
