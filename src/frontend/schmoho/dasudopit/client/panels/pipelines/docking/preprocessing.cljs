(ns schmoho.dasudopit.client.panels.pipelines.docking.preprocessing
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.components.structure :as structure]
   [schmoho.components.pdb :as pdb]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs :as subs]
   [schmoho.dasudopit.client.panels.pipelines.docking.events :as events]
   [reagent.core :as r]))

(def form-model
  {:taxons        [:docking :selected-taxons]
   :current-taxon [:docking :current-taxon]
   :plddt-cutoffs [:docking :pltddt-cutoffs]
   :hydrogenate?  [:docking :hydrogenate?]
   :charges?      [:docking :charges?]
   :cut?          [:docking :cut?]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(defn viewer
  [&{:keys [pdb start end]}]
  [pdb/pdb-viewer
   :structure-reload? true
   :objects {:pdb pdb}
   :style {:cartoon {:colorfunc
                     (fn [atom]
                       (if (< start (.-resi ^js atom) end)
                         "blue"
                         "yellow"))}}
   :config {:backgroundColor "white"}])

(defn protein-plddt-cutoff-chooser
  [&{:keys [pdb]}]
  (let [cutoff-model        (rf/subscribe [::subs/plddt-cutoff])]
    (fn [&{:keys [pdb]}]
      (when pdb
        (let [cutoff              @cutoff-model
              {:keys [start end]} @(rf/subscribe [::subs/plddt-cutoff-indices])]
          [v
           :children
           [[:span "pLDDT cutoff: " cutoff]
            [com/slider
             :model cutoff-model
             :on-change #(rf/dispatch [::events/set-current-taxon-plddt %])]
            [viewer :pdb pdb :start start :end end]]])))))


(defn taxon-protein-lookup-tab-bar
  [taxons]
  [h
   :gap "20px"
   :align :start
   :children
   [[structure/vertical-bar-tabs
     :model (model :current-taxon)
     :id-fn (comp :id :meta)
     :label-fn (comp :name :meta)
     :style {:max-width "250px"
             :width     "250px"}
     :on-change (setter :current-taxon)
     :tabs taxons]]])

(defn preprocessing-form
  []
  (let [taxon-model       (model :taxons)
        current-taxon     @(model :current-taxon)
        taxons-lookup     (rf/subscribe [:data/taxons-map])
        selected-taxons   (->> @taxon-model (map @taxons-lookup) not-empty)
        current-structure (:structure @(rf/subscribe [::subs/current-structure-data]))]
    [h
     :gap "50px"
     :children
     [[taxon-protein-lookup-tab-bar selected-taxons]
      [v
       :children
       [[com/checkbox
         :model (model :cut? current-taxon)
         :on-change (setter :cut? current-taxon)
         :label "Cut tail regions"]
        [com/checkbox
         :model (model :hydrogenate? current-taxon)
         :on-change (setter :hydrogenate? current-taxon)
         :label "Add hydrogen atoms"]
        [com/checkbox
         :model (model :charges? current-taxon)
         :on-change (setter :charges? current-taxon)
         :label "Add Gasteiger-charges"]]]
      (when @(model :cut? current-taxon)
        [protein-plddt-cutoff-chooser :pdb current-structure])]]))


