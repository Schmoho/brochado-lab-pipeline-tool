(ns unknown-client.views.ligand
  (:require
   [unknown-client.subs :as subs]
   [re-frame.core :as re-frame]
   [unknown-client.styles :as styles]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn render-prop [prop]
  (let [urn         (:urn prop)
        value       (:value prop)
        display-val (or (:ival value)
                        (:fval value)
                        (:sval value)
                        (when-let [bin (:binary value)]
                          (str "Binary (" (count bin) " chars)")))]
    [re-com/h-box
     :gap "10px"
     :children [[re-com/label :label (str (:label urn)
                                          (when-let [n (:name urn)]
                                            (str " (" n ")")))]
                [re-com/label :label (str "Value: " display-val)]]]))

(defn json-details [json]
  [re-com/v-box
   :gap "10px"
   :children [[re-com/label :label (str "ID: " (get-in json [:id :id :cid]))]
              [re-com/label :label (str "Charge: " (:charge json))]
              [re-com/title :label "Properties" :level :level2]
              [re-com/v-box :gap "5px" :children (map render-prop (:props json))]
              [re-com/title :label "Count" :level :level2]
              [re-com/label :label (:count json)]]])

(defn ligands-panel []
  (let [results (re-frame/subscribe [:data/ligands])]
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

(defn ligands-header []
  [re-com/title
   :src   (at)
   :label "Ligand Library"
   :level :level1
   :class (styles/header)])

(defn single-ligand-panel []
  (let [params @(re-frame/subscribe [::subs/active-route-params])
        results @(re-frame/subscribe [:data/ligand (:ligands/id params)])
        json-data (:json results)
        png       (:png results)
        img-src   (str "data:image/png;base64," png)]
    [v
     :gap "20px"
     :children
     [[h
       :align :center
       :children
       [[re-com/title :label (:name results)
         :level :level1]
        [re-com/gap :size "1"]
        [:img {:src   img-src
               :style {:max-width  "250px"
                       :max-height "250px"
                       :border     "1px solid #ddd"}}]]]
      [json-details json-data]]]))

(defn single-ligand-header []
  (let [params @(re-frame/subscribe [::subs/active-route-params])
        results @(re-frame/subscribe [:data/ligand (:ligands/id params)])]
    [re-com/title
     :src   (at)
     :label (:name results)
     :level :level1
     :class (styles/header)]))
