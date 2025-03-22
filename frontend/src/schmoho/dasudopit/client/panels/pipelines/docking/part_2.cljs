(ns schmoho.dasudopit.client.panels.pipelines.docking.part-2
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.http :as http]
   [schmoho.dasudopit.client.common.views.forms :as views.forms] 
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.css.forms :as css]
   [schmoho.dasudopit.client.panels.pipelines.docking.utils :as utils]
   [schmoho.dasudopit.client.panels.pipelines.docking.events :as events]))

(defn handle-get-structures-click-fn
  [selected-proteins selected-taxons]
  (fn []
    (doseq [t selected-taxons]
      (rf/dispatch [::forms/set-form-data
                    :docking
                    :input-model
                    :taxon
                    t
                    :plddt-cutoff
                    80]))))

(defn get-structures-button
  []
  (let [hover? (r/atom false)]
    (fn []
      (let [input-model       @(rf/subscribe [:forms.docking/input-model])
            selected-proteins (->> input-model
                                   :taxon
                                   vals
                                   (map (comp :id :protein)))
            selected-taxons   (->> input-model :taxon keys)]
        (when (every? some? selected-proteins)
          [com/button
           :src       (at)
           :label    "GET STRUCTURES"
           :class    (css/rectangle-button)
           :style    {:background-color "#0072bb"}
           :on-click (handle-get-structures-click-fn selected-proteins selected-taxons)
           :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
           :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                      :on-mouse-out  (com/handler-fn (reset! hover? false))}])))))

(defn taxon-protein-lookup
  [taxon]
  (let [protein-model (rf/subscribe [:forms/by-path :docking :input-model :taxon (:id taxon) :protein])
        protein       @(rf/subscribe [:data/protein (:id @protein-model)])
        form-valid?   @(rf/subscribe [:forms.docking.part-2/valid?])
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


(defn upload-pdb-modal
  []
  [com/modal-panel
   :child
   [v
    :children
    [[views.forms/info-label
      "Required: Name"
      [:div ""]]
     [views.forms/input-text
      :on-change #(rf/dispatch [::forms/set-form-data :upload/structure :meta :name %])
      :placeholder "Insert a name for the structure"]
     [com/gap :size "10px"]
     [views.forms/info-label
      "Required: PDB file"
      [volcano-info]]
     [views.forms/pdb-upload
      :on-load #(rf/dispatch [::forms/set-form-data :upload/structure :file %])]
     [h
      :children
      [[com/gap :size "80px"]
       [views.forms/action-button
        :label "Save"
        :on-click #(rf/dispatch [::events/post-structure])]]]]]])


(defn part-2
  []
  (let [form-valid?        @(rf/subscribe [:forms.docking.part-2/valid?])
        taxon-lookup       @(rf/subscribe [:data/taxons-map])
        selected-taxons    (-> @(rf/subscribe [:forms.docking/input-model]) :taxon keys set)
        proteome-searchers (->> selected-taxons
                                (map taxon-lookup)
                                (map
                                 (fn [taxon]
                                   ^{:key (:id taxon)}
                                   [taxon-protein-lookup taxon])))]
    [v
     :children
     [(when-not form-valid?
        [:span "Please choose a protein for each taxon and press the button to get the structures."])
      [h
       :min-height "300px"
       :gap "30px"
       :children
       (into [] proteome-searchers)]
      [get-structures-button]
      #_[upload-pdb-modal]]]))
