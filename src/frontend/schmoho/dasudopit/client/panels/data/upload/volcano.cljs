(ns schmoho.dasudopit.client.panels.data.upload.volcano
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure]
   [schmoho.components.uniprot :as uniprot]))

(def form-model
  {:taxon   [:upload/volcano :meta :taxon]
   :name    [:upload/volcano :meta :name]
   :table   [:upload/volcano :table]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

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
  (let [taxon-choices @(rf/subscribe [:data/taxon-choices])
        taxon-model   (model :taxon)]
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
       :on-load (setter :table)]
      [uniprot/taxon-chooser
         :required? false
         :choices   taxon-choices
         :model     taxon-model
         :on-change (setter :taxon)]
      [structure/flex-horizontal-center
       [components.forms/action-button
         :label    "Save"
         :on-click (fn handle-save-volcano-button
                     []
                     ;; (rf/dispatch [::events/post-volcano])
                     (prn "Do something."))]]]]))
