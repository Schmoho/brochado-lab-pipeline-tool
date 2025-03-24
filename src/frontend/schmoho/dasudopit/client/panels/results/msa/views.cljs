(ns schmoho.dasudopit.client.panels.results.msa.views
  (:require
   ;; ["gosling.js" :refer [GoslingComponent]]
   [clojure.string :as str]
   [re-com.core :as re-com :rename {v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.views.structure :as structure]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.panels.results.msa.plots :as spec]
   [schmoho.dasudopit.client.routing :as routing]))

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
  [structure/header :label "Sequence Comparison Results"])

;; (comment

;;   (require '["gosling.js" :refer [GoslingComponent]])

;;   [:> GoslingComponent
;;    {:spec spec/spec
;;     :margin 0
;;     :padding 30
;;     :border "none"
;;     :id "my-gosling-component-id"
;;     :className "my-gosling-component-style"
;;     :theme "light"}])
