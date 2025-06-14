(ns schmoho.dasudopit.client.panels.pipelines.docking.provide-data
  (:require
   [cljs.pprint :as pprint]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs :as subs]
   [schmoho.utils.core :refer [cool-select-keys]]
   [schmoho.components.uniprot :as uniprot]
   [schmoho.components.forms :as component.forms]
   [schmoho.components.structure :as structure]
   [clojure.string :as str]
   [reagent.core :as r]
   [schmoho.components.pdb :as pdb]))


(def form-model
  {:taxons            [:docking :selected-taxons]
   :current-taxon     [:docking :current-taxon]
   :ligands           [:docking :selected-ligands]
   :proteins          [:docking :selected-proteins]
   :structures        [:docking :selected-structures]
   :binding-sites     [:docking :selected-binding-sites]
   :current-structure [:docking :current-structure]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(defn ligand-multi-choice
  "Reagent component"
  [& {:keys [choices on-change model id-fn label-fn]}]
  (if (empty? choices)
    [v
     :justify :center
     :children
     [[com/throbber
       :size :large
       :style {:width "410px"}]]]
    [com/multi-select :src (at)
     :choices       choices
     :id-fn id-fn
     :label-fn label-fn
     :model         model
     :on-change     #(when on-change
                       (on-change %))
     :width         "47.5%"
     :left-label    "Available ligands"
     :right-label   "Selected ligands"
     :placeholder   "Select at least one ligand."
     :required? true
     :filter-box? false]))

(defn taxon-multi-choice
  [& {:keys [choices on-change model id-fn label-fn]}]
  (if (empty? choices)
    [v
     :justify :center
     :children
     [[com/throbber
       :size :large
       :style {:width "410px"}]]]
    [com/multi-select :src (at)
     :width         "47.5%"
     :id-fn id-fn
     :label-fn label-fn
     :choices       choices
     :model         model
     :on-change     #(when on-change
                       (on-change %))
     :left-label    "Available taxons"
     :right-label   "Selected taxons"
     :placeholder   "Select some taxons."
     :required? true
     :filter-box? false]))

#_(model :binding-sites)

(defn protein-choice-component
  []
  (let [taxon              (model :current-taxon)
        taxons-lookup      (rf/subscribe [:data/taxons-map])
        protein-model      (rf/subscribe [::subs/current-protein-data])
        binding-site-model (rf/subscribe [::subs/current-binding-site])]
    (fn []
      (let [structure            (rf/subscribe [:data/structure (:id @protein-model)])
            input-structures     (get @structure "input")
            processed-structures (get @structure "processed")
            proteome             (-> @taxons-lookup (get @taxon) :proteome)]
        [v
         :gap "10px"
         :children
         [[uniprot/protein-search
           :proteome  proteome
           :model     protein-model
           :on-change #(do
                         (let [proteome (-> @taxons-lookup (get @taxon) :proteome)]
                           (when (some? %)
                             (rf/dispatch [::forms/update-form-data
                                           :docking
                                           :selected-proteins
                                           (fn [selected-proteins]
                                             (merge selected-proteins {@taxon %}))])))
                         (when (:id @protein-model)
                           (db/get-data [:data :structure (:id @protein-model)])))]
          (when @protein-model
            (let [model (r/atom {})]
              [h
               :children
               [[uniprot/protein-structural-features-overview
                 @protein-model
                 :active-site-click-handler
                 #(rf/dispatch [::forms/update-form-data
                                :docking
                                :selected-binding-sites
                                (fn [selected-binding-sites]
                                  (merge selected-binding-sites
                                         {@taxon %}))])
                 :active-site-click-model
                 binding-site-model
                 :badges
                 [(let [input-structures-count (count input-structures)]
                    [component.forms/pill-badge
                     :label (str "User-provided structures"
                                 (when (pos-int? input-structures-count) (str ": " input-structures-count)))
                     :true? (pos-int? input-structures-count)])
                  (let [processed-structures-count (count processed-structures)]
                    [component.forms/pill-badge
                     :label (str "Pre-processed structures"
                                 (when (pos-int? processed-structures-count) (str ": " processed-structures-count)))
                     :true? (pos-int? processed-structures-count)])]]]]))]]))))

(defn handle-set-structure
  [structure taxon-model]
  (do
    (rf/dispatch [::forms/update-form-data
                  :docking
                  :selected-structures
                  (fn [selected-structures]
                    (merge selected-structures {taxon-model structure}))])
    (let [source (name (:source structure))]
      (if (= "afdb" source)
        (db/get-data [:data :structure (:protein structure) source])
        (db/get-data [:data :structure (:protein structure) source (:id structure)])))))

(defn structure-choice-component
  []
  (let [taxon-model       (model :current-taxon)
        protein-model     (rf/subscribe [::subs/current-protein-data])
        choices           @(rf/subscribe [:data/structure-choices @protein-model])
        current-structure @(rf/subscribe [::subs/current-structure-data])]
    [v
     :children
     [[component.forms/dropdown
       :label "Structure"
       :model (:meta current-structure)
       :choices choices
       :on-change #(handle-set-structure % @taxon-model)
       :placeholder "Choose a protein structure"]
      (when current-structure
        [pdb/structural-features-viewer
         :structure-reload? true
         :pdb (:structure current-structure)
         :uniprot @protein-model])]]))

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

(defn taxon-sub-component
  [taxons current-taxon]
  (fn [taxons current-taxon]
    [h
     :gap "10px"
     :children
     [[taxon-protein-lookup-tab-bar taxons]
      [v
       :children
       [[structure/flex-horizontal-center
         [component.forms/eliding-label (-> current-taxon :meta :name)]]
        [h
         :children
         [[protein-choice-component]
          (when @(rf/subscribe [::subs/current-protein-data])
            [:div
             {:style {:width "100%"}}
             [structure-choice-component]])]]]]]]))

(defn handle-set-ligands
  [ligands]
  (rf/dispatch [::forms/set-form-data
                :docking
                :selected-ligands
                (set ligands)]))

(defn handle-set-taxons
  [taxons]
  (doseq [chosen-taxon taxons]   
    (db/get-data [:data :taxon chosen-taxon :proteome]))
  (rf/dispatch [::forms/set-form-data
                :docking
                :selected-taxons
                (set taxons)])
  (rf/dispatch [::forms/update-form-data
                :docking
                :current-taxon
                (fn [current-taxon]
                  (if-not ((set taxons) current-taxon)
                    (first taxons)
                    current-taxon))]))

(defn provide-data-form
  []
  (let [taxons        (rf/subscribe [:data/taxons-list])
        taxons-lookup (rf/subscribe [:data/taxons-map])
        ligands       (rf/subscribe [:data/ligands-list])
        form-valid?   (rf/subscribe [::subs/provided-data-valid?])
        taxon-model   (model :taxons)
        ligand-model  (model :ligands)
        current-taxon (model :current-taxon)]
    (fn []
      [v
       :gap "20px"
       :width "100%"
       :children
       [(when-not @form-valid?
          [:span "Please choose at least one taxon and ligand."])
        [h
         :src      (at)
         :children
         [[taxon-multi-choice
           :choices   @taxons
           :id-fn     (comp :id :meta)
           :label-fn  (comp :name :meta)
           :model     taxon-model
           :on-change #(handle-set-taxons %)]
          [com/gap :size "1"]
          [ligand-multi-choice
           :choices   @ligands
           :id-fn     (comp :cid :meta)
           :label-fn  (comp :title :meta)
           :model     ligand-model
           :on-change #(handle-set-ligands %)]]]
        
        (let [selected-taxons (->> @taxon-model (map @taxons-lookup) not-empty)]
          (when selected-taxons
            [taxon-sub-component selected-taxons (@taxons-lookup @current-taxon) ]))]])))

