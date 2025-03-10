(ns unknown-client.views.upload-data
  (:require
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.events :as events]
   [unknown-client.styles :as styles]
   [unknown-client.subs :as subs]))


(defn upload-data-header []
  [re-com/title
   :src   (at)
   :label "Upload Core Data"
   :level :level1
   :class (styles/header)])

(defn file-upload []
  (let [uploading? (r/atom false)
        file       (r/atom nil)]
    (fn []
      [:div
       [:div {:class "input-group mb-3"}
        [:div {:class "input-group-prepend"}
         [:span {:class "input-group-text"
                 :on-click
                 (when @file
                   (fn []
                     (let [form-data (js/FormData.)]
                       (.append form-data "file" @file)
                       (reset! uploading? true)
                       (-> (js/fetch "/upload" ;; change this URL to your endpoint
                                     (clj->js {:method "POST"
                                               :body   form-data}))
                           (.then (fn [response]
                                      ;; Process the response as needed
                                    (reset! uploading? false)
                                    (js/console.log "Upload complete:" response)))
                           (.catch (fn [error]
                                     (reset! uploading? false)
                                     (js/console.error "Upload failed:" error)))))))}
          (if @uploading? "Uploading..." "Upload")]]
        [:div {:class "custom-file"}
         [:input {:type      "file"
                  :id        "inputGroupFile01"
                  :class     "custom-file-input"
                  :on-change
                  (fn [e]
                    (let [files (-> e .-target .-files)]
                      (when (pos? (.-length files))
                        (reset! file (aget files 0)))))}]
         [:label {:for   "inputGroupFile01"
                  :class "custom-file-label"}
          (if @file (.-name @file) "Choose file")]]]])))

(defn upload-data-panel []
  [h
   :gap "20px"
   :children
   [[re-com/v-box
     :src      (at)
     :children
     [[re-com/title :label "Upload volcano data" :level :level2]
      [h :src (at)
       :gap      "4px"
       :children
       [[:span.field-label "Volcano File"]
        [re-com/info-button
         :src (at)
         :info
         [v :src (at)
          :children
          [[:p.info-heading "Input Volcano File"]
           [:p "You can upload CSV files that satisfy this schema:"]
           [:p (str/join " | "
                         ["effect_size"
                          "effect_type"
                          "log_transformed_f_statistic"
                          "fdr"
                          "gene_name (optional)"
                          "protein_id (optional)"])]
           [re-com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [file-upload]]]
    [re-com/v-box
     :src      (at)
     :children
     [[re-com/title :label "Provision Ligand" :level :level2]
      [h :src (at)
       :gap      "4px"
       :children
       [[:span.field-label "Pubchem Compound ID"]
        [re-com/info-button
         :src (at)
         :info
         [v :src (at)
          :children
          [[:p.info-heading "Pubchem ID"]
           [:p "You need to put in a Pubchem Compound ID. Please note Pubchem distinguishes 'substances' and 'compounds'. We are going for compounds."]
           [re-com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [re-com/input-text
       :model nil
       :on-change (fn [_])]]]
    [re-com/v-box
     :src      (at)
     :children
     [[re-com/title :label "Provision Organism" :level :level2]
      [h :src (at)
       :gap      "4px"
       :children
       [[:span.field-label "Uniprot/NCBI Taxonomy ID"]
        [re-com/info-button
         :src (at)
         :info
         [v :src (at)
          :children
          [[:p.info-heading "Organism ID"]
           [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
           [re-com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [re-com/input-text
       :model nil
       :on-change (fn [_])]]]]])



