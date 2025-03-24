(ns schmoho.dasudopit.client.common.views.protein
  (:require
   [schmoho.utils.color :as color]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]))

(defn- protein-feature->location
  [protein-feature]
  [(-> protein-feature :location :start :value)
   (-> protein-feature :location :end :value)])

(defn- features-with-colors
  [features palette-fn]
  (map
   (fn [domain color]
     (assoc domain :color color))
   (sort-by :description features)
   (palette-fn (count features))))

(defn- location-str
  [feature]
  (let [[start end] (protein-feature->location feature)]
    (if (= start end)
      start
      (str start " - " end))))

(defn- protein-info
  [protein]
  (let [features      (:features protein)
        by-type       (->> features
                           (map (fn [f]
                                  (let [type (:type f)]
                                    {:type         (if (#{"Domain"
                                                          "Topological domain"
                                                          "Transmembrane"} type)
                                                     "Domain"
                                                     type)
                                     :description  (:description f)
                                     :location-str (location-str f)
                                     :location     (protein-feature->location f)})))
                           (group-by :type))
        domains       (features-with-colors (get by-type "Domain") color/green-yellow-palette)
        active-sites  (features-with-colors (get by-type "Active site") color/red-palette)
        binding-sites (features-with-colors (get by-type "Binding site") color/purple-palette)
        has-afdb?     (->> (:uniProtKBCrossReferences protein)
                           (filter #(= "AlphaFoldDB" (:database %)))
                           seq)]
    {:domains       domains
     :active-sites  active-sites
     :binding-sites binding-sites
     :has-afdb?     has-afdb?}))

(defn- feature->hiccup
  [feature-view-data]
  [h
   :gap "5px"
   :children
   [[:div {:style {:width "10px" :height "10px" :background-color (:color feature-view-data)}}]
    [:span "at residue(s)"]
    [:span (:location-str feature-view-data) ":"]
    [:span (:description feature-view-data)]]])

(defn protein-info->hiccup
  [{:keys [has-afdb? domains active-sites binding-sites]}]
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
              [[:span "Not available"]])))])

(defn protein->hiccup
  [protein]
  (let [{:keys [has-afdb? domains active-sites binding-sites]} (protein-info protein)]
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

(defn protein-info->coloring-map
  [protein-info]
  (->> (concat (:domains protein-info)
               (:binding-sites protein-info)
               (:active-sites protein-info))
       (mapcat
        (fn [{:keys [location color]}]
          (zipmap
           (range (first location) (inc (second location)))
           (repeat color))))
       (into {})))

(defn- protein-coloring-fn
  [protein-info]
  (let [coloring-map (protein-info->coloring-map protein-info)]
    (fn [atom]
      (or (get coloring-map (.-resi ^js atom))
          "grey"))))

(defn- protein-info->active-site-balls
  [protein-info]
  (->> protein-info
       :active-sites
       (map (fn [{:keys [location color]}]
              {:resi location :radius 3.0 :color color}))))

(defn protein-site-picker
  [pdb uniprot]
  (let [protein-info      (protein-info uniprot)
        active-site-balls (protein-info->active-site-balls protein-info)
        coloring-fn       (protein-coloring-fn protein-info)]
    [h
     :children
     [[widgets/pdb-viewer
       :objects {:pdb     pdb
                 #_#_:spheres active-site-balls}
       :style {:cartoon {:colorfunc #_coloring-fn
                         (constantly "grey")}}
       :config {:backgroundColor "white"}]
      #_[protein-info->hiccup protein-info]]]))


;; === Taxon Protein Search ===

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


;; @(rf/subscribe [:data/protein "P02919"])


(defn taxon-protein-search
  [& {:keys [show-info? taxon protein-model on-change]
      :or   {show-info? true}}]
  (let [protein  (rf/subscribe [:data/protein (:id @protein-model)])
        proteome (-> @(rf/subscribe [:data/proteome taxon]) :data)]
    [v
     :children
     [[:h6 (:scientificName taxon)]
      [protein-search
       :proteome  proteome
       :model     protein-model
       :on-change #(when on-change (on-change %))]
      (when (and @protein show-info?)
        [protein-info->hiccup (protein-info @protein)])]]))
