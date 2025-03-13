(ns unknown-client.views.results.msa
  (:require
   ["gosling.js" :refer [GoslingComponent]]
   [clojure.string :as str]
   [unknown-client.views.results.alignment-spec :as spec]
   [re-frame.core :as rf]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]
   [unknown-client.views.common.structure :as structure]
   [unknown-client.views.common.widgets :as widgets]))

(defn msa-results-panel
  []
  (let [results (rf/subscribe [:results/msa])]
    (fn []
      [v
       :width "1550px"
       :max-width "1550px"
       :children
       [[widgets/table
         results
         :columns
         [{:id             :id
           :header-label   "Job ID"}
          {:id             :protein-ids
           :header-label   "Protein IDs"
           :row-label-fn   (comp
                            #(str/join ", " %)
                            :protein-ids)}
          {:id             :gene-names
           :header-label   "Gene Names"
           :row-label-fn   (comp
                            #(str/join ", " %)
                            :gene-names)}
          {:id             :blast-still-running?
           :header-label   "Done?"
           :row-label-fn   #(if (:blast-still-running? %)
                              [re-com/throbber :size :small]
                              [:i {:style {:width "40px"} :class "zmdi zmdi-check zmdi-hc-2x"}])}]]
        #_[:> GoslingComponent
         {:spec spec/spec
          :margin 0
          :padding 30
          :border "none"
          :id "my-gosling-component-id"
          :className "my-gosling-component-style"
          :theme "light"}]]])))


(defmethod routing/panels :routing.results/msa [] [msa-results-panel])
(defmethod routing/header :routing.results/msa []
  [structure/header :label "Taxonomic Sequence Comparison Pipeline"])

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
