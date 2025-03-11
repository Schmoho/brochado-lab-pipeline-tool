(ns unknown-client.views.results.msa
  (:require
   ["gosling.js" :refer [GoslingComponent]]
   [clojure.string :as str]
   [unknown-client.views.results.alignment-spec :as spec]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]))


(defn msa-results-header []
  [re-com/title
   :src   (at)
   :label "Taxonomic Sequence Comparison Pipeline"
   :level :level1])


(defn msa-results-panel
  []
  (let [results (re-frame/subscribe [:msa/results])]
    (fn []
      [v
       :width "1550px"
       :max-width "1550px"
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
           :row-label-fn   (comp
                            #(str/join ", " %)
                            :protein-ids)
           :align          "left"
           :width          200
           :vertical-align "middle"}
          {:id             :gene-names
           :header-label   "Gene Names"
           :row-label-fn   (comp
                            #(str/join ", " %)
                            :gene-names)
           :width          200
           :align          "left"
           :vertical-align "middle"}
          {:id             :blast-still-running?
           :header-label   "Done?"
           :row-label-fn   #(if (:blast-still-running? %)
                              [re-com/throbber :size :small]
                              [:i {:style {:width "40px"} :class "zmdi zmdi-check zmdi-hc-2x"}])
           :width          200
           :align          "left"
           :vertical-align "middle"}]
         :row-height                35]
        [:> GoslingComponent
         {:spec spec/spec
          :margin 0
          :padding 30
          :border "none"
          :id "my-gosling-component-id"
          :className "my-gosling-component-style"
          :theme "light"}]]])))


(defmethod routing/panels :msa-results [] [msa-results-panel])
(defmethod routing/header :msa-results [] [msa-results-header])

(comment

  (require '["gosling.js" :refer [GoslingComponent]])

  [:> GoslingComponent
   {:spec spec/spec
    :margin 0
    :padding 30
    :border "none"
    :id "my-gosling-component-id"
    :className "my-gosling-component-style"
    :theme "light"}])
