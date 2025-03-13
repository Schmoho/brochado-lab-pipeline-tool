(ns unknown-client.views.data.upload
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [unknown-client.events.forms :as form-events]
   [unknown-client.events.http :as http-events]
   [unknown-client.routing :as routing]
   [unknown-client.views.common.forms :as common.forms]
   [unknown-client.views.common.structure :refer [card]
    :as structure]
   [unknown-client.views.common.widgets :as widgets]))

(defn volcano-info
  []
  [:<>
   [:p.info-heading "Input Volcano File"]
   [:p "You can upload CSV files that satisfy this schema:"]
   [:p (str/join " | "
                 ["effect_size"
                  "effect_type"
                  "log_transformed_f_statistic"
                  "fdr"
                  "gene_name (optional)"
                  "protein_id (optional)"])]
   [com/hyperlink-href :src (at)
    :label  "Link to docs."
    :href   ""
    :target "_blank"]])

(defn provision-organism-form
  []
  [card
   "Provision Organism via Uniprot"
   [:<>
    [common.forms/info-label
     "Uniprot/NCBI Taxonomy ID"
     [:<>
      [:p.info-heading "Organism ID"]
      [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
      [com/hyperlink-href :src (at)
       :label  "Link to docs."
       :href   ""
       :target "_blank"]]]
    [com/input-text
     :model nil
     :on-change (fn [_])]]])

(defn provision-ligand-form
  []
  [card
   "Provision Ligand via Pubchem"
   [:<>
    [common.forms/info-label
     "Pubchem Compound ID"
     [:<>
      [:p.info-heading "Pubchem ID"]
         [:p "You need to put in a Pubchem Compound ID. Please note Pubchem distinguishes 'substances' and 'compounds'. We are going for compounds."]
         [com/hyperlink-href :src (at)
          :label  "Link to docs."
          :href   ""
          :target "_blank"]]]
    [com/input-text
     :model nil
     :on-change (fn [_])]]])

(defn upload-volcano-form
  []
  [card
   "Upload volcano data"
   ""
   [v
    :children
    [[common.forms/info-label
      "Required: Name"
      [:div ""]]
     [common.forms/input-text
      :on-change #(rf/dispatch [::form-events/set-form-data :upload/volcano :meta :name %])
      :placeholder "Insert a dataset name"]
     [com/gap :size "10px"]
     [common.forms/info-label
      "Required: Volcano File"
      [volcano-info]]
     [common.forms/csv-upload
      :on-load #(rf/dispatch [::form-events/set-form-data :upload/volcano :file %])]
     [common.forms/info-label
      "Optional: Taxon"
      [:<>]]
     [h
      :children
      [[widgets/taxon-chooser
        :on-change
        #(rf/dispatch [::form-events/set-form-data :upload/volcano :meta :taxon %])]
       [com/gap :size "80px"]
       [common.forms/action-button
        :label "Save"
        :on-click #(rf/dispatch [::http-events/post-volcano])]]]]]])

(defn upload-data-panel
  []
  [v
   :gap "20px"
   :children
   [[upload-volcano-form]
    [provision-ligand-form]
    [provision-organism-form]]])


(defmethod routing/panels :routing.data/upload [] [upload-data-panel])
(defmethod routing/header :routing.data/upload []
  [structure/header :label "Upload Data"])
