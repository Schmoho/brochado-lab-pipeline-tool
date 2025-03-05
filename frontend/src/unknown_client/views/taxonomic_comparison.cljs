(ns unknown-client.views.taxonomic-comparison
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.views.defs :as defs]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.common :refer [help-thingie checkbox]]))

(defn taxonomic-comparison-header []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Taxonomic Sequence Comparison Pipeline" )
     :level :level1
     :class (styles/header)]))

;; (defn nav-bar []
;;   [v
;;    :size "70px"
;;    :child "Nav"
;;    :class (styles/nav)
;;    :children
;;    []])

(defn tree-search-form
  []
  (let [form @(re-frame/subscribe [:taxonomic-comparison/form])]
    [v
     :class (styles/form-section)
     :children
     [[:h1 {:class (styles/section-header)} "Taxonomic Search"]
      [v
       :children
       [[checkbox
         {:label      "Search for homologous proteins along the Uniprot Taxonomy tree"
          :model      (-> form :params.uniprot/taxonomy :use-taxonomic-search?)
          :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                           :taxonomic-comparison
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
           :on-change #(re-frame/dispatch [::events/set-form-data
                                           :taxonomic-comparison
                                           :params.uniprot/taxonomy
                                           :top-level
                                           %])]]]
        (when ((set (map :id defs/insane-taxonomic-levels))
               (-> form :params.uniprot/taxonomy :top-level))
          [checkbox 
           {:label      "Really use this taxonomic level?"
            :model      (-> form :params.uniprot/taxonomy :really-use-broad-taxonomic-category?)
            :disabled? (-> form :params.uniprot/taxonomy :use-taxonomic-search? not)          
            :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                             :taxonomic-comparison
                                             :params.uniprot/taxonomy
                                             :really-use-broad-taxonomic-category?])
            :help-title "Hey this is crazy!"
            :help-text  "But the number of results will be huuuge and somebody needs to download all of this."}])]]]]))

(defn blast-form
  []
  (let [form @(re-frame/subscribe [:taxonomic-comparison/form])]
    [v
     :class (styles/form-section)
     :children
     [[:h1 {:class (styles/section-header)} "BLAST"]
      [h
       :children
       [[checkbox
         {:label      "Use BLAST against the Uniprot database"
          :model      (-> form :params.uniprot/blast :use-blast?)
          :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                           :taxonomic-comparison
                                           :params.uniprot/blast
                                           :use-blast?])
          :help-title "What is BLAST?"
          :help-text  "Baby don't hurt me."}]
        [checkbox
         {:label      "Filter BLAST results by taxonomy"
          :disabled? (-> form :params.uniprot/blast :use-blast? not)
          :model      (-> form :params.uniprot/blast :filter-blast-result-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                           :taxonomic-comparison
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
         :on-change #(re-frame/dispatch [::events/set-form-data
                                         :taxonomic-comparison
                                         :params.uniprot/blast
                                         :database
                                         %])]]]]]))

(defn uniref-form
  []
  (let [form @(re-frame/subscribe [:taxonomic-comparison/form])]
    [v
     :class (styles/form-section)
     :children
     [[:h1 {:class (styles/section-header)} "UniRef Search"]
      [h
       :children
       [[checkbox
         {:label      "Use UniRef Cluster search"
          :model      (-> form :params.uniprot/uniref :use-uniref?)
          :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                           :taxonomic-comparison
                                           :params.uniprot/uniref
                                           :use-uniref?])
          :help-title "Oh this is a doozy!"
          :help-text  "Somewhere over the rainbow"}]
        [checkbox
         {:label      "Filter UniRef clusters by taxonomy"
          :disabled? (-> form :params.uniprot/uniref :use-uniref? not)
          :model      (-> form :params.uniprot/uniref :filter-clusters-by-taxonomy?)
          :on-change  #(re-frame/dispatch [::events/toggle-form-bool
                                           :taxonomic-comparison
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
         :on-change #(re-frame/dispatch [::events/set-form-data
                                         :taxonomic-comparison
                                         :params.uniprot/uniref
                                         :cluster-types
                                         %])]]]]]))

(defn inputs
  []
  (let [form @(re-frame/subscribe [:taxonomic-comparison/form])]
    []
    [v
     :class (styles/form-section)
     :children
     [[:h1 {:class (styles/section-header)} "Inputs"]
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
           :on-change #(re-frame/dispatch [::events/set-form-data
                                           :taxonomic-comparison
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
         :on-change #(re-frame/dispatch [::events/set-form-data
                                         :taxonomic-comparison
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
     :class    (styles/inner-page-link)
       :style    {:background-color "#0072bb"}
       :on-click #(re-frame/dispatch [::events/start-taxonomic-comparison!])
       :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
       :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                  :on-mouse-out  (re-com/handler-fn (reset! hover? false))}])))

(defn taxonomic-comparison-form
  []
  [v
   :children
   
   [[re-com/button
     :label "Take a tour"
     :on-click #(re-frame/dispatch [::events/start-a-tour :taxonomic-comparison])]
    [inputs]
    [tree-search-form]
    [blast-form]
    [uniref-form]
    [start-button]]])

(defn taxonomic-comparison-panel []
  #_[v
     :src      (at)

     :children [[taxonomic-comparison-title]
                [common/link-to-page "go to About Page" :about]]]
  [taxonomic-comparison-form])
