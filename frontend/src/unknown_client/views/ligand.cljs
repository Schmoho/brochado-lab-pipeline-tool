(ns unknown-client.views.ligand
  (:require
   [unknown-client.subs :as subs]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn ligands-panel []
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



(defn single-ligand-panel []
  (let [params @(re-frame/subscribe [::subs/active-route-params])
        results @(re-frame/subscribe [:data/ligand (:ligands/id params)])]
    [:div (str results)]))
