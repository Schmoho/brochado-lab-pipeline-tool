(ns schmoho.dasudopit.client.panels.data.taxon
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as re-frame]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.components.structure :as structure]
   [schmoho.components.uniprot :as uniprot]))

(defn taxons-panel []
  (let [results (re-frame/subscribe [:data/taxons-list])]
    (fn []
      (if (empty? @results)
        [com/throbber :size :regular]
        [v
         :width "1550px"
         :max-width "1550px"
         :children
         [[h
           :children
           [[com/simple-v-table
             :src                       (at)
             :model                     results
             :max-width "1000px"
             :columns
             [{:id             :id
               :header-label   "Taxon ID"
               :row-label-fn   (fn [row]
                                 [:a {:href (str "taxon/" (:id row))}
                                  (:id row)])
               :width          300
               :align          "center"
               :vertical-align "middle"}
              {:id             :scientificName
               :header-label   "Name"
               :row-label-fn   :scientificName
               :align          "left"
               :width          300
               :vertical-align "middle"}]
             :row-height                35]]]]]))))


(defn proteome-stats
  []
  (let [params   @(re-frame/subscribe [::routing/active-route-params])
        taxon    @(re-frame/subscribe [:data/taxon (:taxon/id params)])
        {:keys [statistics strains]}
        taxon
        {:keys [reviewedProteinCount unreviewedProteinCount proteomeCount]}
        statistics]
    [v
     :children
     [[com/title :label "Proteome Statistics" :level :level2]
      [com/label :label (str "Reviewed Proteins: " reviewedProteinCount)]
      [com/label :label (str "Unreviewed Proteins: " unreviewedProteinCount)]
      [com/label :label (str "Proteome Count: " proteomeCount)]
      [com/v-box
       :children [[com/title :label "Strains" :level :level2]
                  (map (fn [{:keys [name synonyms]}]
                         [com/h-box
                          :gap "10px"
                          :children [[com/label :label (str name)]
                                     [com/label :label (str "( " (str/join ", " synonyms) " )")]]])
                       strains)]]]]))

(defn bug-fluff
  [lineage]
  [v
   :align :center
   :children
   [[:img {:src   "https://phil.cdc.gov//PHIL_Images/10043/10043_lores.jpg"
           :width "200px"
           :max-width "200px"}]
    [com/gap :size "10px"]
    [com/v-box
     :children
     [[uniprot/lineage-widget lineage]]]]])

(defn single-taxon-panel
  []
  (let [params            @(re-frame/subscribe [::routing/active-route-params])
        proteome          @(re-frame/subscribe [:data/proteome (:taxon/id params)])
        taxon             @(re-frame/subscribe [:data/taxon (:taxon/id params)])
        {:keys [lineage]} taxon]
    [h
     :width "80%"
     :justify :between
     :children
     [[proteome-stats]
      #_[uniprot/protein-search
         :proteome proteome]
      [bug-fluff lineage]]]))

(defn single-taxon-header []
  (let [params @(re-frame/subscribe [::routing/active-route-params])
        taxon  @(re-frame/subscribe [:data/taxon (:taxon/id params)])]
    [structure/header
     :label (:scientificName taxon)]))

(defmethod routing/panels :routing.data/taxon [] [taxons-panel])
(defmethod routing/header :routing.data/taxon []
  [structure/header :label "Taxon Library"])
(defmethod routing/panels :routing.data/taxon-entry [] [single-taxon-panel])
(defmethod routing/header :routing.data/taxon-entry [] [single-taxon-header])
