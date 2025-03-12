(ns unknown-client.views.volcano-viewer
  (:require
   [unknown-client.routing :as routing]
   [unknown-client.events.forms :as form-events]
   [unknown-client.views.plots.volcano :as volcano-plots]
   [unknown-client.views.vega :as vega]
   [clojure.set :as set]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn database-lookup-xform
  [db-name & {:keys [just-take-first?]
              :or {just-take-first? true}}]
  (comp
   (map (juxt :primaryAccession :uniProtKBCrossReferences))
   (map (juxt first (comp (partial map :id)
                          (partial filter #(= db-name (:database %)))
                          second)))
   (filter (comp not-empty second))
   (if just-take-first?
     (map (juxt first (comp first second)))
     (map identity))))

  (defn orthology-mapping-rel
    [uniprot-proteome kegg-proteome]
    (->> (set/join
          (->> uniprot-proteome
               (transduce
                (comp
                 (database-lookup-xform "KEGG")
                 (map (partial zipmap [:uniprot-id :kegg-id])))
                conj
                []))
          (->> kegg-proteome
               (map (comp
                     #(update % :orthology ffirst)
                     #(select-keys % [:kegg-id :orthology])
                     #(set/rename-keys % {:id :kegg-id})))))))

(defn cross-data
  [data-set-1 data-set-2]
  (set/join
   (->> data-set-1
        (map #(set/rename-keys
               %
               {:effect_size                 :effect_size_1
                :effect_type                 :effect_type_1
                :log_transformed_f_statistic :log_transformed_f_statistic_1
                :fdr                         :fdr_1
                :gene_name                   :gene_name_1})))
   (->> data-set-2
        (map
         #(set/rename-keys
           %
           {:effect_size                 :effect_size_2
            :effect_type                 :effect_type_2
            :log_transformed_f_statistic :log_transformed_f_statistic_2
            :fdr                         :fdr_2
            :gene_name                   :gene_name_2})))))

(defn uniprot-kegg-id-mapping-rel
    [uniprot-proteome-1 kegg-proteome-1
     uniprot-proteome-2 kegg-proteome-2]
    (->> (set/join
          (set (filter (comp not-empty :orthology)
                       (set/rename (orthology-mapping-rel
                                    uniprot-proteome-1
                                    kegg-proteome-1)
                                   {:uniprot-id :uniprot-id-1
                                    :kegg-id    :kegg-id-1})))
          (set (filter (comp not-empty :orthology)
                       (set/rename (orthology-mapping-rel
                                    uniprot-proteome-2
                                    kegg-proteome-2)
                                   {:uniprot-id :uniprot-id-2
                                    :kegg-id    :kegg-id-2}))))))

(defn cross-species-data
  [{:keys [data-set-1
           uniprot-proteome-1
           kegg-proteome-1
           data-set-2
           uniprot-proteome-2
           kegg-proteome-2]}]
  (let [mapping-rel   (uniprot-kegg-id-mapping-rel
                       uniprot-proteome-1
                       kegg-proteome-1
                       uniprot-proteome-2
                       kegg-proteome-2)
        by-uniprot-id (-> (group-by :uniprot-id-1 mapping-rel)
                          (update-vals first))
        postfix-keys (fn [k postfix]
                       (keyword (str (name k) postfix)))]
    (->> data-set-1
         (map
          (fn [row]
            (tap> row)
            (let [protein-id (:protein_id row)
                  mapping    (by-uniprot-id protein-id)]
              (-> (update-keys row #(postfix-keys % "_1"))
                  (assoc :protein_id_2
                         (:uniprot-id-2 mapping)
                         :orthology_id
                         (:orthology mapping)
                         :kegg_id_1
                         (:kegg-id-1 mapping)
                         :kegg_id_2
                         (:kegg-id-2 mapping))))))
         (set/join
          (map
           (fn [row]
             (update-keys row #(postfix-keys % "_2")))
           data-set-2)))))

(defn decorate-data
  [data]
  (->> data
       #_(filter (go-term-filtering-fn proteome "GO:0009252"))
       (map
        #(cond
           (:orthology_id %)
           (assoc % :url (str "https://www.kegg.jp/entry/"
                              (:orthology_id %)))
           (:protein_id %)
           (assoc % :url (str "https://www.uniprot.org/uniprotkb/"
                              (:protein_id %)))
           :else %))))

(defn volcano-header []
  [com/title
   :src   (at)
   :label "Proteomics Hits Viewer"
   :level :level1])


(defn dropdown
  [& {:keys [choices on-change placeholder model]}]
  (let [model (or model (r/atom nil))]
    [com/single-dropdown
     :model model
     :choices choices
     :on-change #(do
                   (reset! model %)
                   (on-change %))
     :placeholder placeholder]))

(defn collapsible-accordion
  [body-1 body-2]
  [:div {:class "accordion", :id "accordionExample"}
   ;; First card
   [:div {:class "card"}
    [:div {:class "card-header", :id "headingOne"}
     [:h5 {:class "mb-0"}
      [:button {:class           "btn btn-link"
                :type            "button"
                :data-toggle     "collapse"
                :data-target     "#collapseOne"
                :aria-expanded   "true"
                :aria-controls   "collapseOne"}
       "Collapsible Group Item #1"]]]
    [:div {:id "collapseOne", :class "collapse show", :aria-labelledby "headingOne", :data-parent "#accordionExample"}
     [:div {:class "card-body"}
      body-1]]]

   ;; Second card
   [:div {:class "card"}
    [:div {:class "card-header", :id "headingTwo"}
     [:h5 {:class "mb-0"}
      [:button {:class           "btn btn-link collapsed"
                :type            "button"
                :data-toggle     "collapse"
                :data-target     "#collapseTwo"
                :aria-expanded   "false"
                :aria-controls   "collapseTwo"}
       "Collapsible Group Item #2"]]]
    [:div {:id "collapseTwo", :class "collapse", :aria-labelledby "headingTwo", :data-parent "#accordionExample"}
     [:div {:class "card-body"}
      body-2]]]])


(defn volcano-panel
  []
  (let [volcanos     (rf/subscribe [:data/volcanos])
        volcano-form (rf/subscribe [:forms/volcano-viewer])]
    (fn []
      (let [volcano-1 (get @volcanos (:volcano-1 @volcano-form))
            volcano-2 (get @volcanos (:volcano-2 @volcano-form))]
        [v
         :children
         [[h
           :src      (at)
           :children [[dropdown
                       :model (:volcano-1 @volcano-form)
                       :placeholder "Choose a data set"
                       :choices
                       (map (fn [v] {:id    (-> v :meta :id)
                                     :label (-> v :meta :name)})
                            (vals @volcanos))
                       :on-change
                       #(rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-1 %])]
                      [dropdown
                       :placeholder "Choose a data set"
                       :choices
                       (map (fn [v] {:id    (-> v :meta :id)
                                     :label (-> v :meta :name)})
                            (vals @volcanos))
                       :on-change
                       #(rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-2 %])]]]
          [collapsible-accordion
           (when-let [table (:table volcano-1)]
             [vega/vega-chart "v1" (volcano-plots/single-pan table)])
           (when-let [table (:table volcano-1)]
            [vega/vega-chart "v1" (volcano-plots/single-pan table)])]

          #_(when-let [table (:table volcano-1)]
            [vega/vega-chart "v1" (volcano-plots/single-pan table)])]]))))

(defmethod routing/panels :routing/volcano-viewer [] [volcano-panel])
(defmethod routing/header :routing/volcano-viewer [] [volcano-header])


#_(defn cross-viz
  [data-set-1
   data-set-2
   cross-data-set
   {:keys [title
           cross-plot-params]}]
  (let [data-set-1 (decorate-data data-set-1)
        data-set-2 (decorate-data data-set-2)
        cross-data (decorate-data cross-data-set)]
    [v
     :children
     [[:h3 title]
      [vega/vega-chart "twocross" (two-volcanoes-cross-plot cross-data cross-plot-params)]
      [vega/vega-chart "st1" (standard data-set-1)]
      [vega/vega-chart "st2" (standard data-set-2)]
      [vega/vega-chart "cross" (cross-highlighting cross-data)]]]))

#_[vega/vega-chart vega/spec]
                  #_(cross-viz
                     data/cefotaxime-ecoli
                     data/amikacin-ecoli
                     (cross-data data/cefotaxime-ecoli
                                 data/amikacin-ecoli)
                     {:title "Cefotaxime vs. Amikacin E.Coli"
                      :cross-plot-params
                      {:x-label "Cefotaxime Fold Change"
                       :y-label "Amikacin Fold Change"
                       :width   500
                       :height  500}})
