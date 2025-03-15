(ns unknown-client.views.home
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.events.routing :as routing-events]
   [unknown-client.views.common.structure :refer [clickable-card] :as structure]
   [unknown-client.routing :as routing]))

(defn volcano-card
  []
  [clickable-card "Proteomics Hits Viewer"
   "Look at volcanos!"
   [:<>
    [:p "Use the interactive viewer to search for interesting hits in an omics data set."]
    [:img {:src "/assets/fluff-volcano.jpg"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::routing-events/navigate :routing/volcano-viewer])])

(defn msa-card
  []
  [clickable-card "Multiple Sequence Alignments"
   "Taxonomic Protein Comparison"
   [:<>
    [:p {:class "card-text"} (str "Use BLAST, UniRef or gene name-based search along a taxonomic tree "
                                  "to assess the taxonomic diversity of one or multiple proteins. "
                                  "Focus on domains of interest, identify conserved or non-conserved sites.")]
    [:img {:src "/assets/fluff-alignment.gif"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::routing-events/navigate :routing.pipelines/msa])])

(defn molecular-docking-card
  []
  [clickable-card "Molecular Docking"
   "Comparative Docking"
   [:<>
    [:p "Prepare a set of homologous protein structures for comparative docking, run the docking on your on machine and upload the results to get a nice set of graphics."]
    [:img {:src "/assets/fluff-docking.png"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::routing-events/navigate :routing.pipelines/docking])])

(defn upload-data-card
  []
  [clickable-card "Upload Core Data"
   "Feed the machine"
   [:<>
    [:p "Provision new taxon data, ligand data or more volcanos. "]]
   :on-click
   #(re-frame/dispatch [::routing-events/navigate :routing.data/upload])])

(defn home-panel
  []
  [v
   :children
   [[h
     :gap "20px"
     :children
     [[volcano-card]
      [msa-card]]]
    [h
     :gap "20px"
     :children
     [[molecular-docking-card]
      [upload-data-card]]]]])


(defmethod routing/panels :routing/home [] [home-panel])
(defmethod routing/header :routing/home []
  [structure/header :label "Dario's Super Dope Pipeline Thingy"])
