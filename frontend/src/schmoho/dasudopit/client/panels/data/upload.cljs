(ns schmoho.dasudopit.client.panels.data.upload
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.panels.data.events :as events]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.common.views.forms :as common.forms]
   [schmoho.dasudopit.client.common.http :as http]
   [schmoho.dasudopit.client.common.views.structure :refer [minicard card]
    :as structure]
   [schmoho.dasudopit.client.utils :refer [cool-select-keys]]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]))

;; === Volcano ===

(defn volcano-info
  []
  [:<>
   [:p.info-heading "Input Volcano File"]
   [:p "You can upload CSV files that satisfy this schema:"]
   [:p (str/join " | "
                 ["effect_size"
                  "effect_type"
                  "log_transformed_f_statistic"
                  "fdr"
                  "gene_name (optional)"
                  "protein_id (optional)"])]
   [com/hyperlink-href :src (at)
    :label  "Link to docs."
    :href   ""
    :target "_blank"]])

(defn taxon-chooser
  [& {:keys [on-change]}]
  (let [taxons          (rf/subscribe [:data/taxons-list])
        selection-model (r/atom nil)]
    [com/single-dropdown
     :choices
     (conj (map #(cool-select-keys
                  %
                  [[:id :id]
                   [:label :scientificName]])
                @taxons)
           {:id nil :label "-"})
     :model selection-model
     :on-change #(do
                   (reset! selection-model %)
                   (on-change %))
     :placeholder "For which taxon?"]))

(defn upload-volcano-form
  []
  [card
   "Upload volcano data"
   ""
   [v
    :children
    [[common.forms/info-label
      "Required: Name"
      [:div ""]]
     [common.forms/input-text
      :on-change #(rf/dispatch [::forms/set-form-data :upload/volcano :meta :name %])
      :placeholder "Insert a dataset name"]
     [com/gap :size "10px"]
     [common.forms/info-label
      "Required: Volcano File"
      [volcano-info]]
     [common.forms/csv-upload
      :on-load #(rf/dispatch [::forms/set-form-data :upload/volcano :table %])]
     [common.forms/info-label
      "Optional: Taxon"
      [:<>]]
     [h
      :children
      [[taxon-chooser
        :on-change
        #(rf/dispatch [::forms/set-form-data :upload/volcano :meta :taxon %])]
       [com/gap :size "80px"]
       [common.forms/action-button
        :label "Save"
        :on-click #(rf/dispatch [::events/post-volcano])]]]]]])

;; === Ligand ===

(defn search-ligand-button
  []
  (let [input-model (rf/subscribe [:provision.ligand/input-model])]
    [common.forms/action-button
     :label "Search"
     :on-click #(rf/dispatch [::http/http-get
                              [:data :ligand @input-model :search]])]))


(defn ligand-search-result
  [{:keys [meta png]}]
  [v
   :align :center
   :children
   [[com/title :label (:title meta)
     :level :level4]
    [com/gap :size "1"]
    [:img {:src (str "data:image/png;base64," png)
           :style {:max-width  "250px"
                   :max-height "250px"
                   :border     "1px solid #ddd"}}]]])

(defn ligand-previewer
  []
  (let [search-results (rf/subscribe [:provision.ligand/search-result])
        tab-model      (rf/subscribe [:provision.ligand/tab-model])]
    (fn []
      [v
      :children
      [[com/horizontal-bar-tabs
        :model tab-model
        :on-change #(rf/dispatch [::forms/set-form-data :provision/ligand :tab %])
        :tabs
        (->> @search-results
             (map (fn [[id data]]
                    {:id    id
                     :label (-> data :meta :title)}))
             vec)]
       [ligand-search-result
        (->> @search-results
             (filter
              (fn [[id _]]
                (= @tab-model id)))
             first
             second)]
       [common.forms/action-button
        :label "Save"
        :on-click #(rf/dispatch [::http/http-post [:data :ligand @tab-model]])]]])))


(defn provision-ligand-form
  []
  (let [input-model     (rf/subscribe [:provision.ligand/input-model])
        search-results  (rf/subscribe [:provision.ligand/search-result])
        search-running? (rf/subscribe [:provision.ligand/search-running?])
        post-query-state (rf/subscribe [:provision.ligand/post-query-state])]
    [v
     :children
     [[common.forms/info-label
       "Pubchem Compound ID"
       [:<>
        [:p.info-heading "Compound name or Pubchem ID"]
        [:p (str "If you put in a name, please note that you might have to choose between multiple results. A tab bar will appear and let you choose the options."
                 "If you put in a Pubchem Compound ID, please note Pubchem distinguishes 'substances' and 'compounds'. We are going for compounds.")]
        [com/hyperlink-href :src (at)
         :label  "Link to docs."
         :href   ""
         :target "_blank"]]]
      [com/input-text
       :model input-model
       :on-change #(do (rf/dispatch [::forms/set-form-data :provision/ligand :input %])
                       (rf/dispatch [::forms/set-form-data :provision/ligand :tab nil]))]
      [search-ligand-button]
      (when (= :done @post-query-state)
        [:span "Successfully added ligand " @input-model])
      (when @search-running?
        [com/throbber])
      (when (and (not= :done @post-query-state)
                 (not-empty @search-results))
        [ligand-previewer])]]))

;; === Taxon ===

(defn taxon-minicard
  [taxon]
  [minicard
   "Taxon"
   [v
    :children
    [[:p "ID: " (:id taxon)]
     [:p "Name: " (:scientificName taxon)]
     [:p "Taxonomic rank: " (:rank taxon)]]]])

(defn proteome-minicard
  [proteome]
  [minicard
   "Proteome"
   [v
    :children
    [[:p "ID: " (:id proteome)]
     [:p "Type: " (:proteomeType proteome)]
     [:p "Protein count: " (:proteinCount proteome)]]]])

(defn taxon-previewer
  []
  (let [search-result @(rf/subscribe [:provision.taxon/search-result])
        input-model   (rf/subscribe [:provision.taxon/input-model])
        {:keys [taxon proteome]} search-result]
    [v
     :children
     [[h
       :children
       [[v
         :children
         [[taxon-minicard taxon]
          [proteome-minicard proteome]]]
        [com/gap :size "1"]
        [widgets/lineage-widget (:lineage taxon)]]]
      [common.forms/action-button
       :label "Save"
       :on-click #(rf/dispatch [::http/http-post [:data :taxon @input-model]])]]]))

(defn search-taxon-button
  []
  (let [input-model (rf/subscribe [:provision.taxon/input-model])]
    [common.forms/action-button
     :label "Search"
     :on-click #(rf/dispatch [::http/http-get
                              [:data :taxon @input-model :search]])]))

(defn provision-taxon-form
  []
  (let  [input-model      (rf/subscribe [:provision.taxon/input-model])
         search-results   (rf/subscribe [:provision.taxon/search-result])
         search-running?  (rf/subscribe [:provision.taxon/search-running?])
         post-query-state (rf/subscribe [:provision.taxon/post-query-state])]
    [v
     :children
     [[common.forms/info-label
       "Uniprot/NCBI Taxonomy ID"
       [:<>
        [:p.info-heading "Organism ID"]
        [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
        [com/hyperlink-href :src (at)
         :label  "Link to docs."
         :href   ""
         :target "_blank"]]]
      [com/input-text
       :model input-model
       :on-change #(rf/dispatch [::forms/set-form-data :provision/taxon :input %])]
      [search-taxon-button]
      (when (= :done @post-query-state)
        [:span "Successfully added taxon " @input-model ". Please note it can take half a minute until the proteome is available."])
      (when @search-running?
        [com/throbber])
      (when (and (not= :done @post-query-state)
                 (not-empty @search-results))
        [taxon-previewer])]]))

;; === Structure ===


(defn structure-info
  []
  [:<>
   [:p.info-heading "Input Structure File"]
   [:p "You can upload CSV files that satisfy this schema:"]
   [:p (str/join " | "
                 ["effect_size"
                  "effect_type"
                  "log_transformed_f_statistic"
                  "fdr"
                  "gene_name (optional)"
                  "protein_id (optional)"])]
   [com/hyperlink-href :src (at)
    :label  "Link to docs."
    :href   ""
    :target "_blank"]])

(defn taxon-chooser
  [& {:keys [on-change]}]
  (let [taxons          (rf/subscribe [:data/taxons-list])
        selection-model (r/atom nil)]
    [com/single-dropdown
     :choices
     (conj (map #(cool-select-keys
                  %
                  [[:id :id]
                   [:label :scientificName]])
                @taxons)
           {:id nil :label "-"})
     :model selection-model
     :on-change #(do
                   (reset! selection-model %)
                   (on-change %))
     :placeholder "For which taxon?"]))

(defn taxon-protein-lookup
  [taxon protein-model & {:keys [form-valid?]
                          :or {form-valid? true}}]
  (let [protein       @(rf/subscribe [:data/protein (:id @protein-model)])
        proteome      @(rf/subscribe [:data/proteome (:id taxon)])]
    [v
     :children
     [[:h6 (:scientificName taxon)]
      [widgets/protein-search
       :proteome  proteome
       :model     protein-model
       :on-change #(do
                     (rf/dispatch [::forms/set-form-data
                                  :docking
                                  :input-model
                                  :taxon
                                  (:id taxon)
                                  :protein
                                   %])
                     (rf/dispatch [::http/http-get [:data :structure %]]))]
      (when (and protein (not form-valid?))
        [utils/protein-info-hiccup protein])]]))

(defn upload-structure-form
  []
  [card
   "Upload structure data"
   ""
   [v
    :children
    [[common.forms/info-label
      "Required: Name"
      [:div ""]]
     [common.forms/input-text
      :on-change #(rf/dispatch [::forms/set-form-data :upload/structure :meta :name %])
      :placeholder "Insert a name for the structure"]
     [com/gap :size "10px"]
     [common.forms/info-label
      "Required: Taxon"
      [:<>]]
     [h
      :children
      [[taxon-chooser
        :on-change
        #(rf/dispatch [::forms/set-form-data :upload/structure :meta :taxon %])]
       [com/gap :size "80px"]
       [common.forms/action-button
        :label "Save"
        :on-click (fn [_])]]]
     [com/gap :size "10px"]
     [widgets/protein-search
      :proteome nil
      :on-change nil
      :model nil]
     [com/gap :size "10px"]
     [common.forms/info-label
      "Required: Structure File"
      [structure-info]]
     [common.forms/pdb-upload
      :on-load #(rf/dispatch [::forms/set-form-data :upload/structure :pdb %])]]]])


;; === Main ===

(defn upload-data-panel
  []
  [v
   :gap "20px"
   :children
   [[upload-volcano-form]
    [card
     "Provision Ligand via Pubchem"
     [provision-ligand-form]]
    [card
     "Provision Organism via Uniprot"
     [provision-taxon-form]]
    [card
     "Upload Protein Structure"
     [upload-structure-form]]]])

(defmethod routing/panels :routing.data/upload [] [upload-data-panel])
(defmethod routing/header :routing.data/upload []
  [structure/header :label "Upload Data"])
