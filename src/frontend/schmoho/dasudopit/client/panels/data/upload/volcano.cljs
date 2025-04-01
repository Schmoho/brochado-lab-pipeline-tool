(ns schmoho.dasudopit.client.panels.data.upload.volcano
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure]
   [schmoho.components.uniprot :as uniprot]
   [schmoho.dasudopit.client.http :as http]))

(def form-model
  {:taxon             [:upload/volcano :meta :taxon]
   :name              [:upload/volcano :meta :name]
   :table             [:upload/volcano :table]
   :file-upload-label [:upload/volcano :file-upload-label]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(rf/reg-sub
 :upload.volcano/save-running?
 :<- [::http/queries]
 (fn [queries]
   (= (-> queries :post (get [:data :volcano]))
      :running)))

(rf/reg-sub
 :upload.volcano/post-save-state
 :<- [::http/queries]
 (fn [queries]
   (-> queries :post (get [:data :volcano]))))

(defn- csv-file-info
  []
  [components.forms/info-label
   "Required: Volcano File"
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
     :target "_blank"]]])

(defn upload-volcano-form
  []
  (let [taxon-choices   @(rf/subscribe [:data/taxon-choices])
        taxon-model     (model :taxon)
        save-running?   (rf/subscribe [:upload.volcano/save-running?])
        post-save-state (rf/subscribe [:upload.volcano/post-save-state])]
    [v
     :children
     [[components.forms/input-text
       :width "300px"
       :model (model :name)
       :on-change (setter :name)
       :placeholder "Insert a dataset name"]
      [com/gap :size "10px"]
      [csv-file-info]
      [components.forms/csv-upload
       :label @(model :file-upload-label)
       :on-load #(do
                   ((setter :file-upload-label) %1)
                   ((setter :table) %2))]
      [uniprot/taxon-chooser
       :required? false
       :choices   taxon-choices
       :model     taxon-model
       :on-change (setter :taxon)]
      (when @save-running?
        [structure/flex-horizontal-center
         [com/throbber :size :large]])
      (when (not @save-running?)
        [structure/flex-horizontal-center
         [components.forms/action-button
          :label    "Save"
          :on-click (fn handle-save-volcano-button
                      [_]
                      (rf/dispatch [::http/http-post [:data :volcano]
                                    {:params {:meta  {:name  @(model :name)
                                                      :taxon @(model :taxon)}
                                              :table @(model :table)}}]))]])
      (when (= :done @post-save-state)
        [:span "Successfully added dataset."])]]))
