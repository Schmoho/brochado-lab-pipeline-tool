(ns unknown-client.views.taxon
  (:require
   [unknown-client.subs :as subs]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn taxons-panel []
  (let [results (re-frame/subscribe [:data/taxons])]
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
         :row-height                35]]]]]))


(defn single-taxon-panel []
  (let [params @(re-frame/subscribe [::subs/active-route-params])
        results @(re-frame/subscribe [:data/taxon (:taxons/id params)])
        {:keys [mnemonic scientificName lineage statistics otherNames strains]} results
        {:keys [reviewedProteinCount unreviewedProteinCount proteomeCount]} statistics]
    [re-com/v-box
     :gap "10px"
     :children
     [[re-com/title :label scientificName :level :level1]
      [re-com/h-box
       :children
       [[re-com/v-box
         :children
         [[re-com/v-box
         :gap "5px"
         :children
         (concat [[re-com/title :label "Lineage" :level :level2]]
                 (map (fn [{:keys [scientificName rank taxonId]}]
                        [re-com/h-box
                         :gap "10px"
                         :children [[re-com/label :label rank]
                                    [re-com/label :label scientificName]]])
                      lineage))]
                [re-com/v-box
       :gap "5px"
       :children [[re-com/title :label "Proteome Statistics" :level :level2]
                  [re-com/label :label (str "Reviewed Proteins: " reviewedProteinCount)]
                  [re-com/label :label (str "Unreviewed Proteins: " unreviewedProteinCount)]
                  [re-com/label :label (str "Proteome Count: " proteomeCount)]]]
          
      [re-com/v-box
       :gap "5px"
       :children [[re-com/title :label "Strains" :level :level2]
                  (map (fn [{:keys [name synonyms]}]
                         [re-com/h-box
                          :gap "10px"
                          :children [[re-com/label :label (str name)]
                                     [re-com/label :label (str "( " (clojure.string/join ", " synonyms) " )")]]])
                       strains)]]]]

        [:img {:src "https://phil.cdc.gov//PHIL_Images/10043/10043_lores.jpg"
               :width "300px"
               :height "300px"}]]]
      
]]))

