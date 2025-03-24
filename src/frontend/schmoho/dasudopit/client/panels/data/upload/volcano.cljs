(ns schmoho.dasudopit.client.panels.data.upload.volcano
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure :refer [card]]
   [schmoho.components.uniprot :as uniprot]))

(defn- handle-set-volcano-name
  [name]
  (rf/dispatch [::forms/set-form-data :upload/volcano :meta :name name]))

(defn- volcano-name-chooser
  []
  [v
   :children
   [[components.forms/info-label
   "Required: Name"
   [:div ""]]
   [components.forms/input-text
    :on-change #(handle-set-volcano-name %)
    :placeholder "Insert a dataset name"]]])

(defn- handle-load-csv
  [table]
  (rf/dispatch [::forms/set-form-data :upload/volcano :table table]))

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

(defn- volcano-csv-chooser
  []
  [v
   :children
   [[csv-file-info]
    [components.forms/csv-upload :on-load #(handle-load-csv %)]]])

(defn- handle-choose-taxon-for-volcano
  [taxon]
  (rf/dispatch [::forms/set-form-data :upload/volcano :meta :taxon taxon]))

(defn- volcano-taxon-chooser
  []
  [v
   :children
   [[components.forms/info-label
    "Optional: Taxon"
    [:<>]]
    [uniprot/taxon-chooser
    :model (rf/subscribe [:forms/by-path :upload/volcano :meta :taxon])
    :on-change #(handle-choose-taxon-for-volcano %)]]]1)

(defn- handle-save-volcano-button
  []
  ;; (rf/dispatch [::events/post-volcano])
  (prn "Do something."))

(defn- save-volcano-button
  []
  [components.forms/action-button
   :label "Save"
   :on-click #(handle-save-volcano-button)])

(defn upload-volcano-form
  []
  [card
   "Upload volcano data"
   ""
   [v
    :children
    [[volcano-name-chooser]
     [com/gap :size "10px"]
     [volcano-csv-chooser]
     [h
      :children
      [[volcano-taxon-chooser]
       [com/gap :size "80px"]
       [save-volcano-button]]]]]])
