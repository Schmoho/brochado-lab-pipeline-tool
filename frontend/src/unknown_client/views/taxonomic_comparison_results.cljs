(ns unknown-client.views.taxonomic-comparison-results
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


(defn taxonomic-comparison-results-header []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Taxonomic Sequence Comparison Pipeline" )
     :level :level1
     :class (styles/header)]))

(defn taxonomic-comparison-results-panel
  []
  (let [results (re-frame/subscribe [:taxonomic-comparison/results])]
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
           :header-label   "Job ID"
           :row-label-fn   :id
           :width          300
           :align          "center"
           :vertical-align "middle"}
          {:id             :protein-ids
           :header-label   "Protein IDs"
           :row-label-fn   :protein-ids
           :align          "left"
           :width          200
           :vertical-align "middle"}
          {:id             :gene-names
           :header-label   "Gene Names"
           :row-label-fn   :gene-names
           :width          200
           :align          "left"
           :vertical-align "middle"}
          {:id             :blast-still-running?
           :header-label   "BLAST still running?"
           :row-label-fn   :blast-still-running?
           :width          200
           :align          "left"
           :vertical-align "middle"}]
         :row-height                35
         ]]]]]))







