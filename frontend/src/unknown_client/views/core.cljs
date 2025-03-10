(ns unknown-client.views.core
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.styles :as styles]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.about :as about]
   [unknown-client.views.home :as home]
   [unknown-client.views.volcano :as volcano]
   [unknown-client.views.upload-data :as upload-data]
   [unknown-client.views.structural-comparison :as structural-comparison]
   [unknown-client.views.taxonomic-comparison :as taxonomic-comparison]
   [unknown-client.views.taxonomic-comparison-results :as taxonomic-comparison-results]
   [unknown-client.views.taxon :as taxon]
   [unknown-client.views.ligand :as ligand]
   [unknown-client.views.common :refer [navbar-link]]))

(defmethod routes/panels :home [] [home/home-panel])
(defmethod routes/header :home [] [home/home-header])
(defmethod routes/panels :taxonomic-comparison [] [taxonomic-comparison/taxonomic-comparison-panel])
(defmethod routes/header :taxonomic-comparison [] [taxonomic-comparison/taxonomic-comparison-header])
(defmethod routes/panels :taxonomic-comparison-results [] [taxonomic-comparison-results/taxonomic-comparison-results-panel])
(defmethod routes/header :taxonomic-comparison-results [] [taxonomic-comparison-results/taxonomic-comparison-results-header])
(defmethod routes/panels :structural-comparison [] [structural-comparison/structural-comparison-panel])
(defmethod routes/header :structural-comparison [] [structural-comparison/structural-comparison-header])
(defmethod routes/panels :volcano [] [volcano/volcano-panel])
(defmethod routes/header :volcano [] [volcano/volcano-header])
(defmethod routes/panels :upload-data [] [upload-data/upload-data-panel])
(defmethod routes/header :upload-data [] [upload-data/upload-data-header])

(defmethod routes/panels :taxon [] [taxon/taxons-panel])
(defmethod routes/header :taxon [] [taxon/taxons-header])
(defmethod routes/panels :taxon-entry [] [taxon/single-taxon-panel])
(defmethod routes/header :taxon-entry [] [taxon/single-taxon-header])
(defmethod routes/panels :ligand [] [ligand/ligands-panel])
(defmethod routes/header :ligand [] [ligand/ligands-header])
(defmethod routes/panels :ligand-entry [] [ligand/single-ligand-panel])
(defmethod routes/header :ligand-entry [] [ligand/single-ligand-header])
#_(defmethod routes/panels :protein-entry [] [protein/single-taxon-panel])

(defmethod routes/panels :about [] [about/about-panel])

(defn nav-bar []
  [v
   :class (styles/navbar)
   :children
   [[navbar-link "Home" :home]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Taxons" :taxon]
    [navbar-link "Ligands" :ligand]
    [navbar-link "Upload Core Data" :upload-data]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Volcano Viewer" :taxonomic-comparison]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Taxonomic Protein Comparison" :taxonomic-comparison]
    [navbar-link "Results" :taxonomic-comparison-results]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link  "Comparative Docking" :structural-comparison]
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

(defn brochado-logo
  []
  [:img
   {:src "https://static.wixstatic.com/media/a5509c_a999c4781dd844c5a49a6bc10f87ceb4~mv2.png/v1/fill/w_326,h_84,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/LabLogo_name.png"
    :width "260px"
    :height "auto"}])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [v
     :style {:border "1px solid black"}
     :width "100%"
     :align :stretch
     :gap      "1em"
     :children
     [[h
       :class (styles/header)
       :justify :between
       :children
       [[brochado-logo]
        (routes/header @active-panel)
        [re-com/gap :size "100px"]]]
      [h
       :gap "30px"
       :children
       [[nav-bar]
        (routes/panels @active-panel)]]
      #_[footer]]]))

#_(.reload js/location)
