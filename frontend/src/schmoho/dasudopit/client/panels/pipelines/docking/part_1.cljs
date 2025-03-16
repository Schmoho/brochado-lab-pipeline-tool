(ns schmoho.dasudopit.client.panels.pipelines.docking.part-1
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.utils :as utils :refer [cool-select-keys]]))

;; === Subs ===

(rf/reg-sub
 :forms.docking.part-1/taxon-model
 :<- [:forms/docking]
 (fn [form]
   (set (:taxon-model form))))

(rf/reg-sub
 :forms.docking.part-1/ligand-model
 :<- [:forms/docking]
 (fn [form]
   (set (:ligand-model form))))

(rf/reg-sub
 :forms.docking.part-1/valid?
 :<- [:forms.docking.part-1/taxon-model]
 :<- [:forms.docking.part-1/ligand-model]
 (fn [[taxon ligand]]
   (and (not-empty taxon)
        (not-empty ligand)
        (some? taxon)
        (some? ligand))))

;; === Views ===

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

(defn part-1
  []
  (let [taxons (rf/subscribe [:data/taxons])
        ligands (rf/subscribe [:data/ligands])]
    (fn []
      [v
       :children
       [(when-not @(rf/subscribe [:forms.docking.part-1/valid?])
          [:span "Please choose at least one taxon and ligand."])
        [h
         :src      (at)
         :children
         [[taxon-multi-choice
           :choices
           (taxons->choices @taxons)
           :model
           (rf/subscribe [:forms.docking.part-1/taxon-model])
           :on-change
           #(do
              (doseq [chosen-taxon %]
                (utils/get-data [:data :raw :proteome chosen-taxon]))
              (rf/dispatch
               [::forms/set-form-data
                :docking
                :taxon-model
                %])
              (rf/dispatch
               [::forms/update-form-data
                :docking
                :selected-proteins-model
                (fn [selected-proteins-model]
                  (reduce
                   (fn [acc p]
                     (if (contains? % p)
                       acc
                       (dissoc acc p)))
                   selected-proteins-model
                   (keys selected-proteins-model)))]))]
          [com/gap :size "50px"]
          [ligand-multi-choice
           :choices
           (ligands->choices @ligands)
           :model
           (rf/subscribe [:forms.docking.part-1/ligand-model])
           :on-change
           #(rf/dispatch
             [::forms/set-form-data
              :docking
              :ligand-model
              %])]]]]])))

