(ns schmoho.components.uniprot
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [schmoho.components.forms :as forms]
   [schmoho.components.utils.uniprot :as utils]
   [re-frame.core :as rf]))

;; === Lineage ===

(defn lineage-item [item]
  [v
   :align :center
   :class "btn btn-outline-secondary"
   :children
   [#_[com/label :label (:rank item)]
    [com/hyperlink-href :label (:scientificName item)
     :href (str "https://www.uniprot.org/taxonomy/" (:taxonId item))]]])

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
                    (map lineage-item)
                    (interpose [lineage-connector])
                    reverse
                    vec))])

;; === Taxons ===

(defn taxon-chooser
  [& {:keys [choices on-change model required? info-body]
      :or {required? true
           info-body [:<>]}}]
  [v
   :children
   [[forms/info-label
     (if required?
       "Required: Taxon"
       "Optional: Taxon")
     info-body]
    [com/single-dropdown
     :choices choices
     :model model
     :on-change #(when on-change (on-change %))
     :placeholder "For which taxon?"]]])

;; === Protein Chooser ===

(defn- protein-search-info
  []
  [h
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
        :target "_blank"]]]]]])

(defn- protein-search-suggestions
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

(defn- protein-search
  [& {:keys [proteome on-change model]}]
  (let [suggestions-for-search (protein-search-suggestions proteome)]
    [v
     :src      (at)
     :children
     [[protein-search-info]
      [com/typeahead
       :src (at)
       :model model
       :data-source suggestions-for-search
       :placeholder (str (:protein-name @model))
       :rigid? true
       :change-on-blur? true
       :on-change #(when on-change (on-change %))
       :suggestion-to-string #(:protein-name %)
       :render-suggestion
       (fn [{:keys [protein-name gene-name id]}]
         [:span (str id " - " gene-name " - " protein-name)])]]]))

#_(def proteome (-> @(rf/subscribe [:data/proteome taxon]) :data))

(defn- feature->hiccup
  [feature-view-data]
  [h
   :gap "5px"
   :children
   [[:div {:style {:width "10px" :height "10px" :background-color (:color feature-view-data)}}]
    [:span "at residue(s)"]
    [:span (:location-str feature-view-data) ":"]
    [:span (:description feature-view-data)]]])

(defn protein->hiccup
  [protein]
  (let [{:keys [has-afdb? domains active-sites binding-sites]} (utils/protein-info protein)]
    [v
     :gap "5px"
     :children
     (concat
      [[h
        :gap "5px"
        :children
        [[:span "AlphaFold structure available"]
         (when has-afdb?
           [:i {:class "zmdi zmdi-check"}])]]]
      (into [[:h6 "Domains"]]
            (or (seq (map feature->hiccup domains))
                [[:span "Not available"]]))
      (into [[:h6 "Active Sites"]]
            (or (seq (map feature->hiccup active-sites))
                [[:span "Not available"]]))
      (into [[:h6 "Binding Sites"]]
            (or (seq (map feature->hiccup binding-sites))
                [[:span "Not available"]])))]))

(defn taxon-protein-search
  [& {:keys [:proteome show-info? taxon protein-model on-change]
      :or   {show-info? true}}]
  (let [protein  (rf/subscribe [:data/protein (:id @protein-model)])]
    [v
     :children
     [[:h6 (:scientificName taxon)]
      [protein-search
       :proteome  proteome
       :model     protein-model
       :on-change #(when on-change (on-change %))]
      (when (and @protein show-info?)
        [protein->hiccup @protein])]]))
