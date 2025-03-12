(ns unknown-client.views.pipelines.msa
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.views.pipelines.defs :as defs]
   [unknown-client.routing :as routing]
   [unknown-client.views.css.forms :as css]
   [unknown-client.events.forms :as form-events]
   [unknown-client.views.common.forms :refer [checkbox]]))

(defn msa-header []
  [re-com/title
   :src   (at)
   :label "Taxonomic Sequence Comparison Pipeline"
   :level :level1])

(defn tree-search-form
  []
  (let [form @(re-frame/subscribe [:msa/form])]
    [v
     :class (css/form-section)
     :children
     [[:h1 {:class (css/section-header)} "Taxonomic Search"]
      [v
       :children
       [[checkbox
         {:label      "Search for homologous proteins along the Uniprot Taxonomy tree"
          :model      (-> form :params.uniprot/taxonomy :use-taxonomic-search?)
          :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
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
           :choices defs/all-taxonomic-levels
           :on-change #(re-frame/dispatch [::form-events/set-form-data
                                           :msa
                                           :params.uniprot/taxonomy
                                           :top-level
                                           %])]]]
        (when ((set (map :id defs/insane-taxonomic-levels))
               (-> form :params.uniprot/taxonomy :top-level))
          [checkbox 
           {:label      "Really use this taxonomic level?"
            :model      (-> form :params.uniprot/taxonomy :really-use-broad-taxonomic-category?)
            :disabled? (-> form :params.uniprot/taxonomy :use-taxonomic-search? not)          
            :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
                                             :msa
                                             :params.uniprot/taxonomy
                                             :really-use-broad-taxonomic-category?])
            :help-title "Hey this is crazy!"
            :help-text  "But the number of results will be huuuge and somebody needs to download all of this."}])]]]]))

(defn blast-form
  []
  (let [form @(re-frame/subscribe [:msa/form])]
    [v
     :class (css/form-section)
     :children
     [[:h1 {:class (css/section-header)} "BLAST"]
      [h
       :children
       [[checkbox
         {:label      "Use BLAST against the Uniprot database"
          :model      (-> form :params.uniprot/blast :use-blast?)
          :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
                                           :msa
                                           :params.uniprot/blast
                                           :use-blast?])
          :help-title "What is BLAST?"
          :help-text  "Baby don't hurt me."}]
        [checkbox
         {:label      "Filter BLAST results by taxonomy"
          :disabled? (-> form :params.uniprot/blast :use-blast? not)
          :model      (-> form :params.uniprot/blast :filter-blast-result-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
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
         :choices defs/blast-dbs
         :on-change #(re-frame/dispatch [::form-events/set-form-data
                                         :msa
                                         :params.uniprot/blast
                                         :database
                                         %])]]]]]))

(defn uniref-form
  []
  (let [form @(re-frame/subscribe [:msa/form])]
    [v
     :class (css/form-section)
     :children
     [[:h1 {:class (css/section-header)} "UniRef Search"]
      [h
       :children
       [[checkbox
         {:label      "Use UniRef Cluster search"
          :model      (-> form :params.uniprot/uniref :use-uniref?)
          :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
                                           :msa
                                           :params.uniprot/uniref
                                           :use-uniref?])
          :help-title "Oh this is a doozy!"
          :help-text  "Somewhere over the rainbow"}]
        [checkbox
         {:label      "Filter UniRef clusters by taxonomy"
          :disabled? (-> form :params.uniprot/uniref :use-uniref? not)
          :model      (-> form :params.uniprot/uniref :filter-clusters-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::form-events/toggle-form-bool
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
         :choices defs/uniref-cluster-types
         :on-change #(re-frame/dispatch [::form-events/set-form-data
                                         :msa
                                         :params.uniprot/uniref
                                         :cluster-types
                                         %])]]]]]))

(defn inputs
  []
  (let [form @(re-frame/subscribe [:msa/form])]
    []
    [v
     :class (css/form-section)
     :children
     [[:h1 {:class (css/section-header)} "Inputs"]
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
           :on-change #(re-frame/dispatch [::form-events/set-form-data
                                           :msa
                                           :params.uniprot/protein
                                           :protein-ids
                                           %])]]]
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
         :on-change #(re-frame/dispatch [::form-events/set-form-data
                                         :msa
                                         :params.uniprot/protein
                                         :gene-names
                                         %])]]]]]]]))
        

"Search by taxonomy uses these gene names in tandem with the taxon implied by the protein ID."
"Uniprot. Here be a screenshot that shows you where to get them."

(defn start-button
  []
  (let [hover? (r/atom false)]
    (fn []
      [re-com/button
       :src       (at)
       :label    "START PIPELINE"
       :class    (css/rectangle-button)
       :style    {:background-color "#0072bb"}
       :on-click #(re-frame/dispatch [::form-events/start-msa!])
       :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
       :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                  :on-mouse-out  (re-com/handler-fn (reset! hover? false))}])))

(defn msa-form
  []
  [v
   :children
   [[inputs]
    [tree-search-form]
    [blast-form]
    [uniref-form]
    [start-button]]])

(defn msa-panel []
  [msa-form])

(defmethod routing/panels :msa [] [msa-panel])
(defmethod routing/header :msa [] [msa-header])
