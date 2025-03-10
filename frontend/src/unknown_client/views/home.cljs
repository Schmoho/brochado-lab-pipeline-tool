(ns unknown-client.views.home
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.events :as events]
   [unknown-client.styles :as styles]
   [unknown-client.subs :as subs]))

(defn home-header []
  (let [_ (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label "Dario's Super Dope Pipeline Thingy"
     :level :level1
     :class (styles/header)]))

(defn card
  [header title body & {:keys [on-click]}]
  (let [hover?  (r/atom false)]
    (fn []
      [:div {:class (str "card bg-light mb-3 "
                         (if @hover? (styles/card-hover) ""))
             :style {:width "42rem"}
             :on-mouse-over (re-com/handler-fn (reset! hover? true))
             :on-mouse-out  (re-com/handler-fn (reset! hover? false))
             :on-click on-click}
       [:div {:class "card-header"} header]
       [:div {:class "card-body"}
        [:h5 {:class "card-title"} title]
        body]])))

(defn volcano-card
  []
  [card "Proteomics Hits Viewer"
   "Look at volcanos!"
   [:<>
    [:p "It's amazeballs!"]
    [:img {:src "https://c.pxhere.com/photos/7e/7c/mount_fuji_volcano_japan_sky_sunset_painting-989657.jpg!d"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::events/navigate :volcano])])

(defn msa-card
  []
  [card "Multiple Sequence Alignments"
   "Taxonomic Protein Comparison"
   [:<>
    [:p {:class "card-text"} (str "Use BLAST, UniRef or gene name-based search along a taxonomic tree "
                                  "to assess the taxonomic diversity of one or multiple proteins. "
                                  "Focus on domains of interest, identify conserved or non-conserved sites.")]
    [:img {:src "https://upload.wikimedia.org/wikipedia/commons/7/79/RPLP0_90_ClustalW_aln.gif"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::events/navigate :taxonomic-comparison])])

(defn molecular-docking-card
  []
  [card "Molecular Docking"
   "Comparative Docking"
   [:<>
    [:p "Prepare a set of homologous protein structures for comparative docking, run the docking on your on machine and upload the results to get a nice set of graphics."]
    [:img {:src "https://upload.wikimedia.org/wikipedia/commons/9/97/Docking_representation_2.png"
           :width "400px"
           :height "auto"}]]
   :on-click
   #(re-frame/dispatch [::events/navigate :structural-comparison])])

(defn upload-data-card
  []
  [card "Upload Core Data"
   "Feed the machine"
   [:<>
    [:p "Provision new taxon data, ligand data or more volcanos. "]]
   :on-click
   #(re-frame/dispatch [::events/navigate :upload-data])])

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

