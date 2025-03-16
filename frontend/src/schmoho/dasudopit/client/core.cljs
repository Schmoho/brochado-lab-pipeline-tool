(ns schmoho.dasudopit.client.core
  (:require
   ["react-dom/client" :refer [createRoot]]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.db :as db]
   [schmoho.dasudopit.client.config :as config]
   [schmoho.dasudopit.client.css.core :as css]
   [schmoho.dasudopit.client.panels.data.ligand]
   [schmoho.dasudopit.client.panels.data.overview]
   [schmoho.dasudopit.client.panels.data.protein]
   [schmoho.dasudopit.client.panels.data.subs]
   [schmoho.dasudopit.client.panels.data.taxon]
   [schmoho.dasudopit.client.panels.data.upload]
   [schmoho.dasudopit.client.panels.home]
   [schmoho.dasudopit.client.panels.pipelines.docking.views]
   [schmoho.dasudopit.client.panels.pipelines.msa.subs]
   [schmoho.dasudopit.client.panels.pipelines.msa.views]
   [schmoho.dasudopit.client.panels.results.docking.views]
   [schmoho.dasudopit.client.panels.results.msa.events]
   [schmoho.dasudopit.client.panels.results.msa.views]
   [schmoho.dasudopit.client.panels.volcano-viewer.events]
   [schmoho.dasudopit.client.panels.volcano-viewer.subs]
   [schmoho.dasudopit.client.panels.volcano-viewer.views]
   [schmoho.dasudopit.client.routing :as routing]))

(defn dev-setup
  []
  (when config/debug?
    (println "dev mode")))

(defn navbar-link
  [link-text route]
  (let  [hover?        (r/atom false)
         at-rest-color "#00796b"
         hover-color   "#0072bb"
         active-color  "#4db6ac"]
    (fn []
      (let [active? (= route @(rf/subscribe [::routing/active-panel]))]
        [com/button
         :src      (at)
         :label    link-text
         :on-click #(rf/dispatch
                     (if (coll? route)
                       (into [::routing/navigate]
                             route)
                       [::routing/navigate route]))
         :class    (css/navbar-button)
         :style    {:background-color (if @hover?
                                        (if active?
                                          active-color
                                          hover-color)
                                        (if active?
                                          active-color
                                          at-rest-color))}
         :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                    :on-mouse-out  (com/handler-fn (reset! hover? false))}]))))
(defn nav-bar []
  [v
   :class (css/navbar)
   :children
   [[navbar-link "Home" :routing/home]
    [com/gap :src (at) :size "40px"]
    [navbar-link "Data Overview" :routing.data/overview]
    [navbar-link "Upload Data" :routing.data/upload]
    [com/gap :src (at) :size "40px"]
    [navbar-link "Volcano Viewer" :routing/volcano-viewer]
    [com/gap :src (at) :size "40px"]
    [navbar-link "Taxonomic Protein Comparison" :routing.pipelines/msa]
    [navbar-link "Results" :routing.results/msa]
    [com/gap :src (at) :size "40px"]
    [navbar-link  "Comparative Docking" :routing.pipelines/docking]
    [navbar-link "Results" :routing.results/docking]]])

(defn footer
  []
  [h
   :class (css/footer)
   :children
   [[com/hyperlink-href
     :class (css/footer-link)
     :label "BLAST Documentation"
     :href "https://www.uniprot.org/help/blast-submission"]
    [com/hyperlink-href
     :class (css/footer-link)
     :label "What is UniRef?"
     :href "https://www.uniprot.org/help/uniref"]
    [com/hyperlink-href
     :class (css/footer-link)
     :label "Why BLAST against UniRef?"
     :href "https://www.uniprot.org/help/uniref_blast_use"]
    [com/hyperlink-href
     :class (css/footer-link)
     :label "Find your protein on UniProt"
     :href "https://www.uniprot.org/help/find_your_protein"]]])

(defn brochado-logo
  []
  [:img
   {:src "/assets/logo.png"
    :width "260px"
    :height "auto"}])

(defn main-panel []
  (let [active-panel (rf/subscribe [::routing/active-panel])]
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
        [com/gap :size "100px"]]]
      [h
       :gap "30px"
       :children
       [[nav-bar]
        (routing/panels @active-panel)]]
      #_[footer]]]))


(defonce root
  (createRoot (.getElementById js/document "app")))

(defn ^:dev/after-load mount-root
  []
  (.render root (r/as-element
                 [main-panel])))

(defn init
  []
  (routing/start!)
  (rf/dispatch-sync [::db/initialize-db])
  (dev-setup)
  (mount-root))


#_(.reload js/location)
