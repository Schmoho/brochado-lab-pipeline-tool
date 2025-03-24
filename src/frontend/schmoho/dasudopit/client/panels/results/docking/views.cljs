(ns schmoho.dasudopit.client.panels.results.docking.views
  (:require
   [clojure.string :as str]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.common.views.structure :as structure]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]))

(defn docking-results-panel []
  (let [results (rf/subscribe [:results/docking])]
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
          {:id             :protein-names
           :header-label   "Protein Names"
           :row-label-fn   (comp
                            #(str/join ", " %)
                            :gene-names)}
          {:id             :docking-still-running?
           :header-label   "Done?"
           :row-label-fn   #(if (:docking-still-running? %)
                              [re-com/throbber :size :small]
                              [:i {:style {:width "40px"} :class "zmdi zmdi-check zmdi-hc-2x"}])}]]]])))

(defmethod routing/panels :routing.results/docking [] [docking-results-panel])
(defmethod routing/header :routing.results/docking []
  [structure/header :label "Comparative Docking Results"])
