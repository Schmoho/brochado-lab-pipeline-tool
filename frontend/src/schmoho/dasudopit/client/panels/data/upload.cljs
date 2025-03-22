(ns schmoho.dasudopit.client.panels.data.upload
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.css.structure :as css]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.panels.data.events :as events]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.common.views.forms :as common.forms]
   [schmoho.dasudopit.client.common.http :as http]
   [schmoho.dasudopit.client.common.views.structure :refer [card]
    :as structure]
   [schmoho.dasudopit.client.utils :refer [cool-select-keys]]))

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

(defn provision-organism-form
  []
  [card
   "Provision Organism via Uniprot"
   [:<>
    [common.forms/info-label
     "Uniprot/NCBI Taxonomy ID"
     [:<>
      [:p.info-heading "Organism ID"]
      [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
      [com/hyperlink-href :src (at)
       :label  "Link to docs."
       :href   ""
       :target "_blank"]]]
    [com/input-text
     :model nil
     :on-change (fn [_])]]])

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
   [[com/title :label (:Title meta)
     :level :level4]
    [com/gap :size "1"]
    [:img {:src (str "data:image/png;base64," png)
           :style {:max-width  "250px"
                   :max-height "250px"
                   :border     "1px solid #ddd"}}]]])

(defn ligand-chooser
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
             (map (fn [[_ data]]
                    {:id    (-> data :meta :Title)
                     :label (-> data :meta :Title)}))
             vec)]
       [ligand-search-result
        (->> @search-results
             (filter
              (fn [[_ data]]
                (= @tab-model (-> data :meta :Title))))
             first
             second)]
       [common.forms/action-button
        :label "Save"
        :on-click #(rf/dispatch [:alert @tab-model])]]])))


(defn provision-ligand-form
  []
  (let [input-model    (rf/subscribe [:provision.ligand/input-model])
        search-results (rf/subscribe [:provision.ligand/search-result])]
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
      (when (not-empty @search-results)
        [ligand-chooser])]]))

(rf/reg-event-fx
 :alert
 (fn [_ result]
   (js/alert result)))

(defn taxon-chooser
  [& {:keys [on-change]}]
  (let [taxons          (rf/subscribe [:data/taxons])
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

(defn upload-data-panel
  []
  [v
   :gap "20px"
   :children
   [[upload-volcano-form]
    [card
     "Provision Ligand via Pubchem"
     [provision-ligand-form]]
    [provision-organism-form]]])


(defmethod routing/panels :routing.data/upload [] [upload-data-panel])
(defmethod routing/header :routing.data/upload []
  [structure/header :label "Upload Data"])
