(ns unknown-client.views.data.upload
  (:require
   [clojure.string :as str]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [unknown-client.routing :as routing]
   [unknown-client.utils :refer [cool-select-keys]]
   [unknown-client.views.common.forms :as common.forms]
   [unknown-client.views.common.structure :refer [card]]))

(defn upload-data-header []
  [re-com/title
   :src   (at)
   :label "Upload Core Data"
   :level :level1])

(defn taxon-chooser
  []
  (let [taxons (re-frame/subscribe [:data/taxons])
        selection-model (r/atom nil)]
    [re-com/single-dropdown
     :choices
     (conj (map #(cool-select-keys
                   %
                   [[:id :taxonId]
                    [:label :scientificName]])
                @taxons)
           {:id nil :label "-"})
     :model selection-model
     :on-change #(reset! selection-model %)
     :placeholder "For which taxon?"]))



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
   [re-com/hyperlink-href :src (at)
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
      [re-com/hyperlink-href :src (at)
       :label  "Link to docs."
       :href   ""
       :target "_blank"]]]
    [re-com/input-text
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
         [re-com/hyperlink-href :src (at)
          :label  "Link to docs."
          :href   ""
          :target "_blank"]]]
    [re-com/input-text
     :model nil
     :on-change (fn [_])]]])

(defn upload-volcano-form
  []
  [card
   "Upload volcano data"
   ""
   [:<>
    [common.forms/info-label
     "Required: Volcano File"
     [volcano-info]]
    [common.forms/file-upload]
    [common.forms/info-label
     "Optional: Taxon"
     [:<>]]
    [taxon-chooser]]])

(defn upload-data-panel []
  [v
   :gap "20px"
   :children
   [[upload-volcano-form]
    [provision-ligand-form]
    [provision-organism-form]]])


(defmethod routing/panels :routing.data/upload [] [upload-data-panel])
(defmethod routing/header :routing.data/upload [] [upload-data-header])
