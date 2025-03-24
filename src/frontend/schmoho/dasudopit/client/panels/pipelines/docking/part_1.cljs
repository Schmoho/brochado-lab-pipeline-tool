(ns schmoho.dasudopit.client.panels.pipelines.docking.part-1
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.utils.core :refer [cool-select-keys]]))

(defn ligands->choices
  [ligands]
  (->> ligands
       (mapv #(cool-select-keys
               %
               [[:label :name]
                [:id [:json :id :id :cid]]]))))

(defn taxons->choices
  [taxons]
  (->> taxons
       (mapv #(cool-select-keys
               %
               [[:label :scientificName]
                [:id :id]]))))

(defn ligand-multi-choice
  "Reagent component"
  [& {:keys [choices on-change model]}]
  (if (empty? choices)
    [v
     :justify :center
     :children
     [[com/throbber
       :size :large
       :style {:width "410px"}]]]
    [com/multi-select :src (at)
     :choices       choices
     :model         model
     :on-change     #(when on-change
                       (on-change %))
     :width         "450px"
     :left-label    "Available ligands"
     :right-label   "Selected ligands"
     :placeholder   "Select at least one ligand."
     :required? true
     :filter-box? false]))

(defn taxon-multi-choice
  [& {:keys [choices on-change model]}]
  (if (empty? choices)
    [v
     :justify :center
     :children
     [[com/throbber
       :size :large
       :style {:width "410px"}]]]
    [com/multi-select :src (at)
     :choices       choices
     :model         model
     :on-change     #(when on-change
                       (on-change %))
     :width         "450px"
     :left-label    "Available taxons"
     :right-label   "Selected taxons"
     :placeholder   "Select some taxons."
     :required? true
     :filter-box? false]))

(defn handle-set-ligands
  [ligands]
  (rf/dispatch [::forms/set-form-data
                :docking
                :input-model
                :ligand
                ligands]))

(defn handle-set-taxons
  [taxons]
  (doseq [chosen-taxon taxons]
    (db/get-data [:data :raw :proteome chosen-taxon]))
  (rf/dispatch [::forms/update-form-data
                :docking
                :input-model
                :taxon
                (fn [input-state]
                  (->> taxons
                       (map (fn [taxon-id]
                              (if-let [contained (get input-state taxon-id)]
                                [taxon-id contained]
                                [taxon-id {}])))
                       (into {})))]))

(defn part-1
  []
  (let [taxons       @(rf/subscribe [:data/taxons-list])
        ligands      @(rf/subscribe [:data/ligands-list])
        form-valid?  @(rf/subscribe [:forms.docking.part-1/valid?])
        taxon-model  (rf/subscribe [:forms.docking.part-1/taxon-model])
        ligand-model (rf/subscribe [:forms.docking.part-1/ligand-model])]
    [v
     :children
     [(when-not form-valid?
        [:span "Please choose at least one taxon and ligand."])
      [h
       :src      (at)
       :children
       [[taxon-multi-choice
         :choices   (taxons->choices taxons)
         :model     taxon-model
         :on-change #(handle-set-taxons %)]
        [com/gap :size "50px"]
        [ligand-multi-choice
         :choices   (ligands->choices ligands)
         :model     ligand-model
         :on-change #(handle-set-ligands %)]]]]]))
