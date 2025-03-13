(ns unknown-client.views.common.widgets
    (:require
     [re-com.core :as com :refer [at v-box h-box]
      :rename {v-box v h-box h}]
     [re-frame.core :as rf]
     [reagent.core :as r]
     [reagent.dom :as rdom]
     [unknown-client.utils :refer [cool-select-keys]]))

(defn lineage-item [idx item]
  ^{:key idx}
  [v
   :align :center
   :class "btn btn-outline-secondary"
   :children [#_[com/label :label (:rank item)]
              [com/hyperlink-href :label (:scientificName item)
               :href (str "https://www.uniprot.org/taxonomy/" (:id item))]]])


(defn lineage-connector []
  [:div {:style {:display        "flex"
                 :flex-direction "column"
                 :align-items    "center"
                 :margin         "3px 0 0 0"}}
   [:div {:style {:width            "8px"
                  :height           "8px"
                  :background-color "#ccc"
                  :border-radius    "50%"}}]
   [:div {:style {:width            "2px"
                  :height           "10px"
                  :background-color "#ccc"}}]])


(defn lineage-widget [lineage]
  [v
   :children (if (empty? lineage)
               [[com/label :label "No lineage available"]]
               (->> lineage
                    (map-indexed lineage-item)
                    (interpose [lineage-connector])
                    reverse
                    vec))])

(defn protein-search-suggestions
  [proteome]
  (fn [s]
    (let [fragment (re-pattern (str "(?i)" (or s "")))]
      (->> proteome
           (keep (fn [protein]
                   (let [gene-name    (or (-> protein :genes first :geneName :value)
                                          (-> protein :genes first :orderedLocusNames :value)
                                          "-")
                         protein-name (or (-> protein :proteinDescription :recommendedName :fullName :value)
                                          (-> protein :proteinDescription :submissionNames first :fullName :value)
                                          "-")
                         protein-id   (or (-> protein :primaryAccession)
                                          "-")]
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (take 6)
           (into [{:id nil :protein-name "-" :gene-name nil}])))))

(defn protein-search
  [& {:keys [proteome on-change model]}]
  (let [suggestions-for-search (protein-search-suggestions proteome)]
    [v
     :src      (at)
     :children
     [[h
       :src (at)
       :children
       [[:span.field-label "Search Proteins"]
        [com/info-button
         :src (at)
         :info
         [v
          :src (at)
          :children
          [[:p.info-heading "Organism ID"]
           [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
           [com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [com/typeahead
       :src (at)
       :model model
       :rigid? true
       :change-on-blur? true
       :on-change
       #(when on-change
          (on-change %))
       :data-source suggestions-for-search
       :suggestion-to-string #(:protein-name %)
       :render-suggestion
       (fn [{:keys [protein-name gene-name id]}]
         [:span (str id " - " gene-name " - " protein-name)])
       :parts
       {:suggestions-container {:style {:z-index 10}}
        :suggestion {:style {:z-index 10}}}]]]))

(defn protein-search-choices
  [proteome]
  (fn [s]
    (let [fragment (re-pattern (str "(?i)" (or s "")))]
      (->> proteome
           (keep (fn [protein]
                   (let [gene-name    (or (-> protein :genes first :geneName :value)
                                          (-> protein :genes first :orderedLocusNames :value)
                                          "-")
                         protein-name (or (-> protein :proteinDescription :recommendedName :fullName :value)
                                          (-> protein :proteinDescription :submissionNames first :fullName :value)
                                          "-")
                         protein-id   (or (-> protein :primaryAccession)
                                          "-")]
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (into [])))))

(defn protein-search-2
  [proteome]
  (let [model                  (r/atom nil)
        suggestions-for-search ((protein-search-choices proteome))]
    (fn []
      [v
       :src      (at)
       :children
       [[h
         :src (at)
         :children
         [[:span.field-label "Search Proteins"]
          [com/info-button
           :src (at)
           :info
           [v
            :src (at)
            :children
            [[:p.info-heading "Organism ID"]
             [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
             [com/hyperlink-href :src (at)
              :label  "Link to docs."
              :href   ""
              :target "_blank"]]]]]]
        [com/single-dropdown
         :src (at)
         :width "100%"
         :choices suggestions-for-search
         :model model
         :on-change #(reset! model %)
         :label-fn :protein-name
         :filter-box? true
         :render-fn
         (fn [{:keys [protein-name gene-name id]}]
           [:span (str id " - " gene-name " - " protein-name)])]]])))

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

(defn pdb-viewer
  [& {:keys [pdb style config on-load]}]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (let [el     (rdom/dom-node this)
            viewer (.createViewer js/$3Dmol el (clj->js config))]
        (doto viewer
          (.addModel pdb "pdb")
          (.setStyle (clj->js {}) (clj->js style))
          (.zoomTo)
          (.render)
          (.zoom 1.2 1000))
        (when on-load
          (on-load viewer))))
    :reagent-render
    (fn []
      [:div {:class "mol-container"
             :style {:width    "60%"
                     :height   "400px"
                     :position "relative"
                     :border "solid grey 1px"}}
       "Loading viewer..."])}))


(defn table
  [data & {:keys [columns]}]
  (if (nil? @data)
    [com/throbber :size :regular]
    [v
     :width "1550px"
     :max-width "1550px"
     :children
     [[h
       :children
       [[com/simple-v-table
         :src                       (at)
         :model data
         :max-width "1000px"
         :columns
         (mapv (fn [defaults input]
                 (merge defaults input))
               (map (fn [col]
                      (assoc
                       {:width 300
                        :align "center"
                        :vertical-align "middle"}
                       :row-label-fn #((:id col) %)
                       :header-label (name (:id col))))
                    columns)
               columns)
         :row-height                35]]]]]))
