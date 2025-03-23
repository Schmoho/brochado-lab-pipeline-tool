(ns schmoho.dasudopit.client.panels.data.upload.volcano
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.forms :as common.forms]
   [schmoho.dasudopit.client.common.views.structure
    :as structure
    :refer [card]]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.panels.data.events :as events]))

(defn- handle-set-volcano-name
  [name]
  (rf/dispatch [::forms/set-form-data :upload/volcano :meta :name name]))

(defn- volcano-name-chooser
  []
  [v
   :children
   [[common.forms/info-label
   "Required: Name"
   [:div ""]]
   [common.forms/input-text
    :on-change #(handle-set-volcano-name %)
    :placeholder "Insert a dataset name"]]])

(defn- handle-load-csv
  [table]
  (rf/dispatch [::forms/set-form-data :upload/volcano :table table]))

(defn- csv-file-info
  []
  [common.forms/info-label
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
    [common.forms/csv-upload :on-load #(handle-load-csv %)]]])

(defn- handle-choose-taxon-for-volcano
  [taxon]
  (rf/dispatch [::forms/set-form-data :upload/volcano :meta :taxon taxon]))

(defn- volcano-taxon-chooser
  []
  [v
   :children
   [[common.forms/info-label
    "Optional: Taxon"
    [:<>]]
   [widgets/taxon-chooser
    :model (rf/subscribe [:forms/by-path :upload/volcano :meta :taxon])
    :on-change #(handle-choose-taxon-for-volcano %)]]])

(defn- handle-save-volcano-button
  []
  (rf/dispatch [::events/post-volcano]))

(defn- save-volcano-button
  []
  [common.forms/action-button
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
