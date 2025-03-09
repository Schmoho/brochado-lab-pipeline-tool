(ns unknown-client.views.core
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [reagent.core :as r]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.about :as about]
   [unknown-client.views.home :as home]
   [unknown-client.views.structural-comparison :as structural-comparison]
   [unknown-client.views.taxonomic-comparison :as taxonomic-comparison]
   [unknown-client.views.taxonomic-comparison-results :as taxonomic-comparison-results]
   [unknown-client.views.taxon :as taxon]
   [unknown-client.views.ligand :as ligand]
   [unknown-client.views.common :refer [navbar-link link-to-page help-thingie checkbox]]))

(defmethod routes/panels :home [] [home/home-panel])
(defmethod routes/header :home [] [home/home-header])
(defmethod routes/panels :taxonomic-comparison [] [taxonomic-comparison/taxonomic-comparison-panel])
(defmethod routes/header :taxonomic-comparison [] [taxonomic-comparison/taxonomic-comparison-header])
(defmethod routes/panels :taxonomic-comparison-results [] [taxonomic-comparison-results/taxonomic-comparison-results-panel])
(defmethod routes/header :taxonomic-comparison-results [] [taxonomic-comparison-results/taxonomic-comparison-results-header])
(defmethod routes/panels :structural-comparison [] [structural-comparison/structural-comparison-panel])
(defmethod routes/header :structural-comparison [] [structural-comparison/structural-comparison-header])

(defmethod routes/panels :taxon [] [taxon/taxons-panel])
(defmethod routes/panels :taxon-entry [] [taxon/single-taxon-panel])
(defmethod routes/panels :ligand [] [ligand/ligands-panel])
(defmethod routes/panels :ligand-entry [] [ligand/single-ligand-panel])
#_(defmethod routes/panels :protein-entry [] [protein/single-taxon-panel])

(defmethod routes/panels :about [] [about/about-panel])

(defn nav-bar []
  [v
   :class (styles/navbar)
   :children
   [[navbar-link "Home" :home]
    [re-com/gap :src (at) :size "60px"]
    [navbar-link "Taxons" :taxon]
    [navbar-link "Ligands" :ligand]
    [re-com/gap :src (at) :size "60px"]
    [navbar-link "Taxonomic Comparison" :taxonomic-comparison]
    [navbar-link "Results" :taxonomic-comparison-results]
    [re-com/gap :src (at) :size "60px"]
    [navbar-link  "Structural Comparison" :structural-comparison]
    [navbar-link "Results" :structural-comparison-results]]])

(defn footer
  []
  [h
   :class (styles/footer)
   :children
   [[re-com/hyperlink-href
     :class (styles/footer-link)
     :label "BLAST Documentation"
     :href "https://www.uniprot.org/help/blast-submission"]
    [re-com/hyperlink-href
     :class (styles/footer-link)
     :label "What is UniRef?"
     :href "https://www.uniprot.org/help/uniref"]
    [re-com/hyperlink-href
     :class (styles/footer-link)
     :label "Why BLAST against UniRef?"
     :href "https://www.uniprot.org/help/uniref_blast_use"]
    [re-com/hyperlink-href
     :class (styles/footer-link)
     :label "Find your protein on UniProt"
     :href "https://www.uniprot.org/help/find_your_protein"]]])



(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [v
     :gap      "1em"
     :children [(routes/header @active-panel)
                [h
                 :children [[nav-bar]
                            [re-com/v-box
                             :src      (at)
                             :height   "100%"
                             :gap      "1em"
                             :children [(routes/panels @active-panel)]]]]
                #_[footer]]]))

#_(.reload js/location)
