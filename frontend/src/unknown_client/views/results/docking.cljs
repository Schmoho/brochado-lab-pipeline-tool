(ns unknown-client.views.results.docking
  (:require
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]))

(defn docking-results-header []
  [re-com/title
   :src   (at)
   :label "Structural Comparison Results"
   :level :level1
   ])

(defn docking-results-panel []
  [:div
   {:id "bla"
    :class "viewer_3Dmoljs"
    :style {:height "600px"
            :width "600px"
            :position  "relative"}
                ;; :width "600px"
                ;; :height "600px"
    :data-pdb "7LQ6"
    :data-backgroundcolor "0xffffff"
    :data-style "cartoon"
    :data-ui true}])

(defmethod routing/panels :docking-results [] [docking-results-panel])
(defmethod routing/header :docking-results [] [docking-results-header])

#_(js->clj js/$3Dmol)
