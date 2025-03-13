(ns unknown-client.views.core
  (:require
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [unknown-client.events.routing :as routing-events]
   [unknown-client.routing :as routing]
   [unknown-client.subs.routing :as routing-subs]
   [unknown-client.views.css.core :as css]
   [unknown-client.views.data.ligand]
    unknown-client.views.data.data-overview
   [unknown-client.views.data.protein]
   [unknown-client.views.data.taxon]
   [unknown-client.views.data.upload]
   [unknown-client.views.home]
   [unknown-client.views.pipelines.docking]
   [unknown-client.views.pipelines.msa]
   [unknown-client.views.results.docking]
   [unknown-client.views.results.msa]
   [unknown-client.views.volcano-viewer]))

(defn navbar-link
  [link-text route]
  (let  [hover?  (r/atom false)
         at-rest-color "#00796b"
         hover-color "#0072bb"
         active-color "#4db6ac"]
    (fn []
      (let [active? (= route @(rf/subscribe [::routing-subs/active-panel]))]
        [re-com/button
         :src       (at)
         :label    link-text
         :on-click #(rf/dispatch
                     (if (coll? route)
                       (into [::routing-events/navigate]
                             route)
                       [::routing-events/navigate route]))
         :class    (css/navbar-button)
         :style    {:background-color (if @hover?
                                        (if active?
                                          active-color
                                          hover-color)
                                        (if active?
                                          active-color
                                          at-rest-color))}
         :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                    :on-mouse-out  (re-com/handler-fn (reset! hover? false))}]))))
(defn nav-bar []
  [v
   :class (css/navbar)
   :children
   [[navbar-link "Home" :routing/home]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Overview Core Data" :routing.data/overview]
    [navbar-link "Upload Core Data" :routing.data/upload]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Volcano Viewer" :routing/volcano-viewer]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link "Taxonomic Protein Comparison" :routing.pipelines/msa]
    [navbar-link "Results" :routing.results/msa]
    [re-com/gap :src (at) :size "40px"]
    [navbar-link  "Comparative Docking" :routing.pipelines/docking]
    [navbar-link "Results" :routing.results/docking]]])

(defn footer
  []
  [h
   :class (css/footer)
   :children
   [[re-com/hyperlink-href
     :class (css/footer-link)
     :label "BLAST Documentation"
     :href "https://www.uniprot.org/help/blast-submission"]
    [re-com/hyperlink-href
     :class (css/footer-link)
     :label "What is UniRef?"
     :href "https://www.uniprot.org/help/uniref"]
    [re-com/hyperlink-href
     :class (css/footer-link)
     :label "Why BLAST against UniRef?"
     :href "https://www.uniprot.org/help/uniref_blast_use"]
    [re-com/hyperlink-href
     :class (css/footer-link)
     :label "Find your protein on UniProt"
     :href "https://www.uniprot.org/help/find_your_protein"]]])

(defn brochado-logo
  []
  [:img
   {:src "https://static.wixstatic.com/media/a5509c_a999c4781dd844c5a49a6bc10f87ceb4~mv2.png/v1/fill/w_326,h_84,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/LabLogo_name.png"
    :width "260px"
    :height "auto"}])

(defn main-panel []
  (let [active-panel (rf/subscribe [::routing-subs/active-panel])]
    [v
     :width "100%"
     :align :stretch
     :gap      "1em"
     :children
     [[h
       :class (css/header)
       :justify :between
       :children
       [[brochado-logo]
        (routing/header @active-panel)
        [re-com/gap :size "100px"]]]
      [h
       :gap "30px"
       :children
       [[nav-bar]
        (routing/panels @active-panel)]]
      #_[footer]]]))

#_(.reload js/location)
