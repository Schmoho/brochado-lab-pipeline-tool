(ns unknown-client.views.data.taxon
  (:require
   [clojure.string :as str]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as re-frame]
   [unknown-client.routing :as routing]
   [unknown-client.subs.routing :as routing-subs]
   [unknown-client.views.common.widgets :as widgets]))

(defn taxons-panel []
  (let [results (re-frame/subscribe [:data/taxons])]
    (fn []
      (if (empty? @results)
        [re-com/throbber :size :regular]
        [v
         :width "1550px"
         :max-width "1550px"
         :children
         [[h
           :children
           [[re-com/simple-v-table
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

(defn taxons-header []
  [re-com/title
   :src   (at)
   :label "Taxon Library"
   :level :level1])


(defn proteome-stats
  []
  (let [params   @(re-frame/subscribe [::routing-subs/active-route-params])
        taxon    @(re-frame/subscribe [:data/taxon (:taxon/id params)])
        {:keys [statistics strains]}
        taxon
        {:keys [reviewedProteinCount unreviewedProteinCount proteomeCount]}
        statistics]
    [v
     :children
     [[re-com/title :label "Proteome Statistics" :level :level2]
      [re-com/label :label (str "Reviewed Proteins: " reviewedProteinCount)]
      [re-com/label :label (str "Unreviewed Proteins: " unreviewedProteinCount)]
      [re-com/label :label (str "Proteome Count: " proteomeCount)]
      [re-com/v-box
       :children [[re-com/title :label "Strains" :level :level2]
                  (map (fn [{:keys [name synonyms]}]
                         [re-com/h-box
                          :gap "10px"
                          :children [[re-com/label :label (str name)]
                                     [re-com/label :label (str "( " (str/join ", " synonyms) " )")]]])
                       strains)]]]]))

(defn bug-fluff
  [lineage]
  [v
   :align :center
   :children
   [[:img {:src   "https://phil.cdc.gov//PHIL_Images/10043/10043_lores.jpg"
           :width "200px"
           :max-width "200px"}]
    [re-com/gap :size "10px"]
    [re-com/v-box
     :children
     [[widgets/lineage-widget lineage]]]]])

(defn single-taxon-panel
  []
  (let [params            @(re-frame/subscribe [::routing-subs/active-route-params])
        proteome          @(re-frame/subscribe [:data/proteome (:taxon/id params)])
        taxon             @(re-frame/subscribe [:data/taxon (:taxon/id params)])
        {:keys [lineage]} taxon]
    [h
     :width "80%"
     :justify :between
     :children
     [[proteome-stats]
      [widgets/protein-search proteome]
      [bug-fluff lineage]]]))

(defn single-taxon-header []
  (let [params @(re-frame/subscribe [::routing-subs/active-route-params])
        taxon  @(re-frame/subscribe [:data/taxon (:taxon/id params)])]
    [re-com/title
     :src   (at)
     :label (:scientificName taxon)
     :level :level1]))

(defmethod routing/panels :routing.data/taxon [] [taxons-panel])
(defmethod routing/header :routing.data/taxon [] [taxons-header])
(defmethod routing/panels :routing.data/taxon-entry [] [single-taxon-panel])
(defmethod routing/header :routing.data/taxon-entry [] [single-taxon-header])
