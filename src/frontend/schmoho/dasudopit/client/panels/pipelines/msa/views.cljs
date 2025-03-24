(ns schmoho.dasudopit.client.panels.pipelines.msa.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.css.forms :as css]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.forms :refer [checkbox] :as common.forms]
   [schmoho.dasudopit.client.common.views.structure :as structure]))

;; defs at the end of this file
(declare blast-dbs)
(declare uniref-cluster-types)
(declare insane-taxonomic-levels)
(declare all-taxonomic-levels)

(defn tree-search-form
  []
  (let [form @(re-frame/subscribe [:forms/msa])]
    [v
     :class (css/form-section)
     :children
     [[v
       :children
       [[checkbox
         {:label      "Search for homologous proteins along the Uniprot Taxonomy tree"
          :model      (-> form :params.uniprot/taxonomy :use-taxonomic-search?)
          :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                           :msa
                                           :params.uniprot/taxonomy
                                           :use-taxonomic-search?])
          :help-title "What tree? You okay buddy?"
          :help-text  "No really, there is a tree!"}]
        [h
         :children
         [[re-com/label :label "Highest Taxonomic Category"
           :style {:padding "5px 5px"}]
          [re-com/single-dropdown
           :placeholder "Which is the highest level taxonomic category you are interested in?"
           :model (-> form :params.uniprot/taxonomy :top-level)
           :disabled? (-> form :params.uniprot/taxonomy :use-taxonomic-search? not)
           :choices all-taxonomic-levels
           :on-change #(re-frame/dispatch [::forms/set-form-data
                                           :msa
                                           :params.uniprot/taxonomy
                                           :top-level
                                           %])]]]
        (when ((set (map :id insane-taxonomic-levels))
               (-> form :params.uniprot/taxonomy :top-level))
          [checkbox 
           {:label      "Really use this taxonomic level?"
            :model      (-> form :params.uniprot/taxonomy :really-use-broad-taxonomic-category?)
            :disabled? (-> form :params.uniprot/taxonomy :use-taxonomic-search? not)          
            :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                             :msa
                                             :params.uniprot/taxonomy
                                             :really-use-broad-taxonomic-category?])
            :help-title "Hey this is crazy!"
            :help-text  "But the number of results will be huuuge and somebody needs to download all of this."}])]]
      [v
         :children
         [[h :src (at)
           :gap      "4px"
           :children [[:span.field-label "Gene Names"]
                      [re-com/info-button
                       :src (at)
                       :info [v :src (at)
                              :children [[:p.info-heading "Input Gene Names"]
                                         [re-com/hyperlink-href :src (at)
                                          :label  "Link to docs."
                                          :href   ""
                                          :target "_blank"]]]]]]
          [re-com/input-textarea
           :model (-> form :params.uniprot/protein :gene-names)
         :on-change #(re-frame/dispatch [::forms/set-form-data
                                         :msa
                                         :params.uniprot/protein
                                         :gene-names
                                         %])]]]]]))

(defn blast-form
  []
  (let [form @(re-frame/subscribe [:forms/msa])]
    [v
     :class (css/form-section)
     :children
     [[h
       :children
       [[checkbox
         {:label      "Use BLAST against the Uniprot database"
          :model      (-> form :params.uniprot/blast :use-blast?)
          :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                           :msa
                                           :params.uniprot/blast
                                           :use-blast?])
          :help-title "What is BLAST?"
          :help-text  "Baby don't hurt me."}]
        [checkbox
         {:label      "Filter BLAST results by taxonomy"
          :disabled? (-> form :params.uniprot/blast :use-blast? not)
          :model      (-> form :params.uniprot/blast :filter-blast-result-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                           :msa
                                           :params.uniprot/blast
                                           :filter-blast-result-by-taxonomy?])
          :help-title "What is BLAST?"
          :help-text  "Baby don't hurt me."}]]]
      [h
       :children
       [[re-com/label :label "Target Database"
         :style {:padding "5px 5px"}]
        [re-com/single-dropdown
         :placeholder "Which Uniprot BLAST database do you want to search?"
         :model (-> form :params.uniprot/blast :database)
         :disabled? (-> form :params.uniprot/blast :use-blast? not)
         :choices blast-dbs
         :on-change #(re-frame/dispatch [::forms/set-form-data
                                         :msa
                                         :params.uniprot/blast
                                         :database
                                         %])]]]]]))

(defn uniref-form
  []
  (let [form @(re-frame/subscribe [:forms/msa])]
    [v
     :class (css/form-section)
     :children
     [[h
       :children
       [[checkbox
         {:label      "Use UniRef Cluster search"
          :model      (-> form :params.uniprot/uniref :use-uniref?)
          :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                           :msa
                                           :params.uniprot/uniref
                                           :use-uniref?])
          :help-title "Oh this is a doozy!"
          :help-text  "Somewhere over the rainbow"}]
        [checkbox
         {:label      "Filter UniRef clusters by taxonomy"
          :disabled? (-> form :params.uniprot/uniref :use-uniref? not)
          :model      (-> form :params.uniprot/uniref :filter-clusters-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::forms/toggle-form-bool
                                           :msa
                                           :params.uniprot/uniref
                                           :filter-clusters-by-taxonomy?])
          :help-title "Oh this is a doozy!"
          :help-text  "Somewhere over the rainbow"}]]]

      [h
       :children
       [[re-com/label :label "UniRef Cluster Type"
         :style {:padding "5px 5px"}]
        [re-com/selection-list
         :model (-> form :params.uniprot/uniref :cluster-types)
         :disabled? (-> form :params.uniprot/uniref :use-uniref? not)
         :choices uniref-cluster-types
         :on-change #(re-frame/dispatch [::forms/set-form-data
                                         :msa
                                         :params.uniprot/uniref
                                         :cluster-types
                                         %])]]]]]))

(defn inputs
  []
  (let [form @(re-frame/subscribe [:forms/msa])]
    [h
     :children
     [[v
       :style {:padding "0px 50px"}
       :children
       [[h :src (at)
         :gap      "4px"
         :children [[:span.field-label "Protein IDs"]
                    [re-com/info-button
                     :src (at)
                     :info [v :src (at)
                            :children [[:p.info-heading "Input Protein IDs"]
                                       [re-com/hyperlink-href :src (at)
                                        :label  "Link to docs."
                                        :href   ""
                                        :target "_blank"]]]]]]
        [re-com/input-textarea
         :model (-> form :params.uniprot/protein :protein-ids)
         :on-change #(re-frame/dispatch [::forms/set-form-data
                                         :msa
                                         :params.uniprot/protein
                                         :protein-ids
                                         %])]]]]]))
        

"Search by taxonomy uses these gene names in tandem with the taxon implied by the protein ID."
"Uniprot. Here be a screenshot that shows you where to get them."

(defn msa-form
  []
  [structure/collapsible-accordion-2
   ["1. Choose taxons and proteins" [inputs]]
   ["2. Configure taxonomic tree search"     [tree-search-form]]
   ["3. Configure BLAST search"     [blast-form]]
   ["4. Configure UniRef search"     [uniref-form]]
   ["5. Set domains of interest"]
   [[common.forms/action-button
     :label "START PIPELINE"
     :on-click #(re-frame/dispatch [::forms/start-msa!])]]])

(defn msa-panel []
  [msa-form])

(defmethod routing/panels :routing.pipelines/msa [] [msa-panel])
(defmethod routing/header :routing.pipelines/msa []
  [structure/header
   :label
   "Sequence Comparison Pipeline"])


;; defs

(def blast-dbs
  [{:id :uniprot-bacteria :label "Uniprot Bacteria"}
   {:id :uniprot-complete-microbial-proteomes :label "Uniprot Complete Microbial Proteomes"}
   {:id :uniprotkb :label "UniprotKB "}
   {:id :uniparc :label "UniParc"}
   {:id :uniref-100 :label "Uniref 100"}
   {:id :uniref-50 :label "Uniref 50"}
   {:id :uniref-90 :label "Uniref 90"}
   {:id :swissprot :label "Swissprot"}
   {:id :trembl :label "TrEMBL"}
   {:id :uniprot-pdb :label "Uniprot PDB"}
   {:id :uniprot-archaea :label "Uniprot Archaea"}
   {:id :uniprot-arthropoda :label "Uniprot Arthropoda"}
   {:id :uniprot-eukaryota :label "Uniprot Eukaryota"}
   {:id :uniprot-fungi :label "Uniprot Fungi"}
   {:id :uniprot-human :label "Uniprot Human"}
   {:id :uniprot-mammals :label "Uniprot Mammals"}
   {:id :uniprot-nematoda :label "Uniprot Nematoda"}         
   {:id :uniprot-rodents :label "Uniprot Rodents"}
   {:id :uniprot-vertebrates :label "Uniprot Vertebrates"}
   {:id :uniprot-viridiplantae :label "Uniprot Viridiplantae"}
   {:id :uniprot-viruses :label "Uniprot Viruses"}])

(def uniref-cluster-types
  [{:id :uniref-100 :label "UniRef 100"}
   {:id :uniref-90 :label "UniRef 90"}
   {:id :uniref-50 :label "UniRef 50"}])

(def insane-taxonomic-levels
  [{:id :no-rank :label "no rank"}
   {:id :phylum :label "phylum"}
   {:id :class :label "class"}
   {:id :order :label "order"}
   {:id :family :label "family"}
   {:id :superkingdom :label "superkingdom"}
   {:id :kingdom :label "kingdom"}
   {:id :clade :label "clade"}
   {:id :subkingdom :label "subkingdom"}
   {:id :subphylum :label "subphylum"}
   {:id :subclass :label "subclass"}
   {:id :suborder :label "suborder"}
   {:id :subfamily :label "subfamily"}
   {:id :tribe :label "tribe"}])

(def all-taxonomic-levels
  (concat [{:id :species :label "species"}
           {:id :strain :label "strain"}]
          insane-taxonomic-levels))
