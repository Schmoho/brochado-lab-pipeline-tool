(ns schmoho.components.uniprot
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [schmoho.components.forms :as forms]
   [schmoho.components.css.forms :as css.forms]
   [schmoho.components.structure :refer [minicard]]
   [schmoho.components.utils.uniprot :as utils]
   [re-frame.core :as rf]
   [schmoho.biodb.uniprot.core :as uniprot]
   [reagent.core :as r]))

;; === Lineage ===

(defn lineage-item [item]
  [v
   :width "200px"
   :align :center
   :class "btn btn-outline-secondary"
   :children
   [[com/hyperlink-href
     :style {:white-space "normal"
             :word-wrap "break-word"}
     :label (:scientificName item)
     :href (str "https://www.uniprot.org/taxonomy/" (:taxonId item))
     :target "_blank"]]])

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
  [& {:keys [choices on-change model required? info-body style width filter-box?]
      :or {required? true
           info-body [:<>]
           width "300px"
           filter-box? true}}]
  [v
   :children
   [[forms/info-label
     (if required?
       "Required: Taxon"
       "Optional: Taxon")
     info-body]
    [com/single-dropdown
     :filter-box? filter-box?
     :style style
     :width width
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
       [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]]]]]])

(defn- protein-search-suggestions
  [proteome]
  (fn [s]
    (let [fragment (re-pattern (str "(?i)" (or s "")))]
      (->> proteome
           :data
           (keep (fn [[protein-id protein]]
                   (let [gene-name    (or (-> protein :genes first :geneName :value)
                                          (-> protein :genes first :orderedLocusNames :value)
                                          "-")
                         protein-name (or (-> protein :proteinDescription :recommendedName :fullName :value)
                                          (-> protein :proteinDescription :submissionNames first :fullName :value)
                                          "-")
                         protein-id   (or protein-id "-")]
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (take 6)
           (into [{:id nil :protein-name "-" :gene-name nil}])))))



(defn- color-rect
  [color]
  [:div {:style {:width "10px"
                 :height "10px"
                 :background-color color
                 :border "1px solid grey"}}])

(defn- feature->hiccup
  [feature-view-data]
  (let [info  [h
               :gap "5px"
               :style {:white-space "pre"}
               :align :center
               :children
               [
                [color-rect (:color feature-view-data)]
                #_[:span "at residue(s)"]
                [:span {:style {:font-family "monospace"}} (:location-str feature-view-data) ":"]
                [:span {:style {:white-space "normal"}}
                 (:description feature-view-data)]]]]
    (if-let [checkbox (:checkbox feature-view-data)]
      (let [model (r/atom nil)]
        [com/checkbox
        :model model
        :on-change #(swap! model not)
        :label info])
      info)))

(defn protein-structural-features-overview
  [protein & {:keys [badges]}]
  (let [{:keys [has-afdb? domains active-sites binding-sites]} (utils/protein-info protein)]
    [minicard
     [h
      :children
      [[:span "Structural Features"]
       [com/gap :size "1"]
       [:a {:href (str "https://www.uniprot.org/uniprotkb/" (:id protein)) :target "_blank"} (:id protein)]]]
     [v
      :style {:font-size "12px"}
      :gap "5px"
      :children
      (concat
       [[h
         :gap "5px"
         :align :center
         :justify :end
         :children
         (into [[h
                 :children
                 [[:span {:class "badge badge-secondary"} "AlphaFold structure available"]
                  (if has-afdb?
                    [:i {:class "zmdi zmdi-check-circle"
                         :style {:color "green"}}]
                    [:i {:class "zmdi zmdi-block"
                         :style {:color "red"}}])]]]
               badges)]]
       (into [[:h6 "Domains"]]
               (or (seq (map feature->hiccup domains))
                   [[:span "Not available"]]))
       (into [[:h6 "Active Sites"]]
             (or (seq (map feature->hiccup active-sites))
                 [[:span "Not available"]]))
       (into [[:h6 "Binding Sites"]]
             (or (seq (map feature->hiccup binding-sites))
                 [[:span "Not available"]])))]]))

(defn protein-search
  [& {:keys [proteome model on-change]}]
  (let [suggestions-for-search (protein-search-suggestions proteome)]
    [v
     :children
     [[protein-search-info]
      [com/typeahead
       :src (at)
       :model                model
       :data-source          suggestions-for-search
       :placeholder          (uniprot/protein-name @model)
       :rigid?               true
       :change-on-blur?      true
       :on-change            #(when on-change (on-change %))
       :suggestion-to-string #(:protein-name %)
       :render-suggestion
       (fn [{:keys [protein-name gene-name id]}]
         [:span (str id " - " gene-name " - " protein-name)])
       :parts {:suggestions-container {:style {:z-index "10"
                                               :background-color "white"}
                                       :class (css.forms/suggestions)}
               :suggestion            {:style {:z-index "10"
                                               :background-color "white"}
                                       :class (css.forms/suggestions)}}]]]))
