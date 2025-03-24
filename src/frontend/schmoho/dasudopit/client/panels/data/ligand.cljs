(ns schmoho.dasudopit.client.panels.data.ligand
  (:require
   [schmoho.dasudopit.client.routing :as routing]
   [re-frame.core :as re-frame]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [schmoho.components.structure :as structure]))

(defn render-prop [prop]
  (let [urn         (:urn prop)
        value       (:value prop)
        display-val (or (:ival value)
                        (:fval value)
                        (:sval value)
                        (when-let [bin (:binary value)]
                          (str "Binary (" (count bin) " chars)")))]
    [h
     :gap "10px"
     :children [[com/label :label (str (:label urn)
                                          (when-let [n (:name urn)]
                                            (str " (" n ")")))]
                [com/label :label (str "Value: " display-val)]]]))

(defn json-details [json]
  [v
   :gap "10px"
   :children [[com/label :label (str "ID: " (get-in json [:id :id :cid]))]
              [com/label :label (str "Charge: " (:charge json))]
              [com/title :label "Properties" :level :level2]
              [v :gap "5px" :children (map render-prop (:props json))]
              [com/title :label "Count" :level :level2]
              [com/label :label (:count json)]]])

(defn ligands-panel []
  (let [results (re-frame/subscribe [:data/ligands-list])]
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
           :header-label   "Ligand ID"
           :row-label-fn   (fn [row]
                             [:a {:href (str "ligand/" (:id row))}
                              (:id row)])
           :width          300
           :align          "center"
           :vertical-align "middle"}
          {:id             :name
           :header-label   "Name"
           :row-label-fn   :name
           :align          "left"
           :width          300
           :vertical-align "middle"}]
         :row-height                35]]]]])  )



(defn single-ligand-panel []
  (let [params    @(re-frame/subscribe [::routing/active-route-params])
        results   @(re-frame/subscribe [:data/ligand (:ligand/id  params)])
        json-data (:json results)
        png       (:png results)
        img-src   (str "data:image/png;base64," png)]
    [v
     :gap "20px"
     :children
     [[h
       :align :center
       :children
       [[com/title :label (:name results)
         :level :level1]
        [com/gap :size "1"]
        [:img {:src   img-src
               :style {:max-width  "250px"
                       :max-height "250px"
                       :border     "1px solid #ddd"}}]]]
      [json-details json-data]]]))

(defn single-ligand-header []
  (let [params @(re-frame/subscribe [::routing/active-route-params])
        results @(re-frame/subscribe [:data/ligand (:ligand/id params)])]
    [com/title
     :src   (at)
     :label (:name results)
     :level :level1]))

(defmethod routing/panels :routing.data/ligand [] [ligands-panel])
(defmethod routing/header :routing.data/ligand []
  [structure/header :label "Ligands Library"])
(defmethod routing/panels :routing.data/ligand-entry [] [single-ligand-panel])
(defmethod routing/header :routing.data/ligand-entry [] [single-ligand-header])
