(ns schmoho.dasudopit.client.panels.pipelines.docking.provide-data
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.utils.core :refer [cool-select-keys]]
   [schmoho.components.uniprot :as uniprot]
   [schmoho.components.forms :as component.forms]))

(def form-model
  {:tab [:docking :tab]})

(def setter (partial forms/setter form-model))

(rf/reg-sub
 :form.docking.provide-data/tab-model
 :<- [:forms/by-path :docking]
 :<- [:forms.docking.provide-data/taxon-model]
 (fn [[form taxon-model]]
   (or (:tab form)
       (-> taxon-model first))))

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
     :width         "47.5%"
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
     :width         "47.5%"
     :choices       choices
     :model         model
     :on-change     #(when on-change
                       (on-change %))
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
    (db/get-data [:data :taxon chosen-taxon :proteome]))
  (rf/dispatch [::forms/update-form-data
                :docking
                :input-model
                :taxon
                (fn [input-state]
                  (prn "taxons: " taxons)
                  (prn "input state: " input-state)
                  (prn "select-keys: " (select-keys input-state taxons))
                  (into (sorted-map)
                        (merge
                         (zipmap taxons (repeat nil))
                         (select-keys input-state taxons))))]))


(defn protein-choice-component
  [taxon]
  (let [taxon-id             (-> taxon :meta :id)
        protein-model        (rf/subscribe [:forms/by-path :docking :input-model :taxon taxon-id :protein])
        protein              (rf/subscribe [:data/protein (:id @protein-model)])
        proteome             (:data @(rf/subscribe [:data/proteome taxon-id]))
        structure            (rf/subscribe [:data/structure (:id @protein)])
        input-structures     (get @structure "input")
        processed-structures (get @structure "processed")]
    [v
     :gap "10px"
     :children
     [[component.forms/eliding-label (-> taxon :meta :name)]
      [uniprot/protein-search
       :proteome  proteome
       :model     protein-model
       :on-change #(do
                     (rf/dispatch [::forms/set-form-data
                                   :docking
                                   :input-model
                                   :taxon
                                   taxon-id
                                   :protein
                                   %])
                     (rf/dispatch [::forms/set-form-data
                                   :docking
                                   :input-model
                                   :taxon
                                   taxon-id
                                   :structure
                                   @structure])
                     (when (:id @protein-model)
                       (db/get-data [:data :structure (:id @protein-model)])))]
      (when @protein
        [uniprot/protein-structural-features-overview
         @protein
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
             :true? (pos-int? processed-structures-count)])]])]]))

(defn taxon-protein-lookup-tab-bar
  [taxons]
  (let [tab-model (rf/subscribe [:form.docking.provide-data/tab-model])
        tabs (->> taxons (mapv (comp (fn [{:keys [id name]}]
                                     {:id id
                                      :label name})
                                     :meta)))
        taxon-lookup @(rf/subscribe [:data/taxons-map])]
    [:<>
     [com/vertical-bar-tabs
       :model tab-model
       :on-change (setter :tab)
      :tabs tabs]
     [v
      :gap "10px"
      :children
      [
       [protein-choice-component (get taxon-lookup @tab-model)]]]]))

(defn provide-data-form
  []
  (let [taxons       (rf/subscribe [:data/taxon-choices])
        ligands      (rf/subscribe [:data/ligand-choices])
        form-valid?  (rf/subscribe [:forms.docking.provide-data/valid?])
        taxon-model  (rf/subscribe [:forms.docking.provide-data/taxon-model])
        ligand-model (rf/subscribe [:forms.docking.provide-data/ligand-model])
        taxon-lookup (rf/subscribe [:data/taxons-map])]
    (fn []
      [v
       :width "100%"
       :children
       [(when-not @form-valid?
          [:span "Please choose at least one taxon and ligand."])
        [h
         :src      (at)
         :children
         [[taxon-multi-choice
           :choices   @taxons
           :model     taxon-model
           :on-change #(handle-set-taxons %)]
          [com/gap :size "1"]
          [ligand-multi-choice
           :choices   @ligands
           :model     ligand-model
           :on-change #(handle-set-ligands %)]]]
        [:p (with-out-str (prn @(rf/subscribe [:forms/docking])))]
        [taxon-protein-lookup-tab-bar (->> @taxon-model (map @taxon-lookup))]]])))
