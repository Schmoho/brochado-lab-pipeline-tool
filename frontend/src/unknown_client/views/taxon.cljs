(ns unknown-client.views.taxon
  (:require
   ["gosling.js" :refer [GoslingComponent]]
   [clojure.string :as str]
   [unknown-client.subs :as subs]
   [unknown-client.views.alignment-spec :as spec]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [unknown-client.styles :as styles]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn taxons-panel []
  (let [results (re-frame/subscribe [:data/taxons])]
    [v
     :width "1550px"
     :max-width "1550px"
     :children
     [[h
       :children
       [[re-com/simple-v-table
         :src                       (at)
         :model                     results
         :max-width "1000px"
         :columns
         [{:id             :id
           :header-label   "Taxon ID"
           :row-label-fn   (fn [row]
                             [:a {:href (str "taxon/" (:id row))}
                              (:id row)])
           :width          300
           :align          "center"
           :vertical-align "middle"}
          {:id             :scientificName
           :header-label   "Name"
           :row-label-fn   :scientificName
           :align          "left"
           :width          300
           :vertical-align "middle"}]
         :row-height                35]]]]]))

(defn taxons-header []
  [re-com/title
   :src   (at)
   :label "Taxon Library"
   :level :level1
   :class (styles/header)])

(defn lineage-item [idx item]
  ^{:key idx}
  [re-com/v-box
   :align :center
   :class "btn btn-outline-secondary"
   :children [#_[re-com/label :label (:rank item)]
              [re-com/hyperlink-href :label (:scientificName item)
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
  [re-com/v-box
   :children (if (empty? lineage)
               [[re-com/label :label "No lineage available"]]
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
                     (prn gene-name)
                     (prn protein-name)
                     (prn protein-id)
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (take 16)
           (into [])))))

(defn protein-search []
  (let [params                 @(re-frame/subscribe [::subs/active-route-params])
        proteome               @(re-frame/subscribe [:data/proteome (:taxons/id params)])
        suggestions-for-search (protein-search-suggestions proteome)]
    [v
     :src      (at)
     :children
     [[h
       :src (at)
       :children
       [[:span.field-label "Search Proteins"]
        [re-com/info-button
         :src (at)
         :info
         [v
          :src (at)
          :children
          [[:p.info-heading "Organism ID"]
           [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
           [re-com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [re-com/typeahead
       :src (at)
       :data-source suggestions-for-search
       :suggestion-to-string #(:protein-name %)
       :render-suggestion
       (fn [{:keys [protein-name gene-name id]}]
         [re-com/hyperlink-href
          :label (str id " - " gene-name " - " protein-name)
          :href (str "/protein/" id)])
       #_#_:on-change (re-frame/dispatch)]]]))

(defn proteome-stats
  []
  (let [params   @(re-frame/subscribe [::subs/active-route-params])
        taxon    @(re-frame/subscribe [:data/taxon (:taxons/id params)])
        {:keys [statistics strains]}
        taxon
        {:keys [reviewedProteinCount unreviewedProteinCount proteomeCount]}
        statistics]
    [v
     :children
     [[re-com/title :label "Proteome Statistics" :level :level2]
      [re-com/label :label (str "Reviewed Proteins: " reviewedProteinCount)]
      [re-com/label :label (str "Unreviewed Proteins: " unreviewedProteinCount)]
      [re-com/label :label (str "Proteome Count: " proteomeCount)]
      [re-com/v-box
       :children [[re-com/title :label "Strains" :level :level2]
                  (map (fn [{:keys [name synonyms]}]
                         [re-com/h-box
                          :gap "10px"
                          :children [[re-com/label :label (str name)]
                                     [re-com/label :label (str "( " (str/join ", " synonyms) " )")]]])
                       strains)]]]]))

(defn bug-fluff
  []
  (let [params            @(re-frame/subscribe [::subs/active-route-params])
        taxon             @(re-frame/subscribe [:data/taxon (:taxons/id params)])
        {:keys [lineage]} taxon]
    [v
     :align :center
     :children
     [[:img {:src   "https://phil.cdc.gov//PHIL_Images/10043/10043_lores.jpg"
             :width "200px"
             :max-width "200px"}]
      [re-com/gap :size "10px"]
      [re-com/v-box
       :children
       [[lineage-widget lineage]]]]]))

(defn single-taxon-panel
  []
  [h
     :width "80%"
     :justify :between
     :children
     [[proteome-stats]
      [protein-search]
      [bug-fluff]]])

(defn single-taxon-header []
  (let [params @(re-frame/subscribe [::subs/active-route-params])
        taxon  @(re-frame/subscribe [:data/taxon (:taxons/id params)])]
    [re-com/title
     :src   (at)
     :label (:scientificName taxon)
     :level :level1
     :class (styles/header)]))

(comment
  [:> GoslingComponent
    {:spec spec/spec
     :margin 0
     :padding 30
     :border "none"
     :id "my-gosling-component-id"
     :className "my-gosling-component-style"
     :theme "light"}])
