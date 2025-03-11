(ns unknown-client.views.data.volcano
  (:require
   [unknown-client.routing :as routing]
   [unknown-client.views.vega :as vega]
   [clojure.set :as set]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]))

(defn standard
  [data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data      {:values data}
   :params    [{:name   "p_effect_type",
                :select {:type "point", :fields ["effect_type"]},
                :bind   "legend"}
               {:name  "p_fdr_threshold",
                :value 0.10,
                :bind  {:input "range", :min 0, :max 1, :step 0.01}}
               {:name   "grid",
                :select "interval",
                :bind   "scales"}]
   :encoding  {:x       {:field "effect_size",
                         :type  "quantitative",
                         :title "Effect Size"},
               :y       {:field "log_transformed_f_statistic",
                         :type  "quantitative",
                         :title "Log2(F-statistic + 1)"}
               :size {:value 60}
               :color   {:field "effect_type",
                         :type  "nominal"
                         :title "Effect type"}
               :opacity {:condition [{:test "(datum.fdr <= p_fdr_threshold)",
                                      :value 1}
                                     #_{:param "p_effect_type", :value 1}]
                         :value 0.2}
               :tooltip [{:field "gene_name", :title "Gene Name" :type "nominal"}
                         {:field "effect_size", :title "Fold Change" :type "quantitative"}]
               :href    {:field "url", :type "nominal"}}
   :mark      {:type "point" :filled "true"}
   :width     600,
   :height    400})

(defn cross-highlighting
  [data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data    {:values data}
   :hconcat [{:width  700,
              :height 600,
              :layer  [{:params   [{:name   "selectedGenes",
                                    :select {:type "interval", :encodings ["x" "y"] :empty "none"}}
                                   {:name   "p_effect_type",
                                    :select {:type "point", :fields ["effect_type_1"]},
                                    :bind   "legend"}
                                   {:name  "p_fdr_threshold",
                                    :value 0.05,
                                    :bind  {:input "range", :min 0, :max 1, :step 0.01}}
                                   #_{:name   "grid",
                                      :select "interval",
                                      :bind   "scales"}],
                        :mark     {:type "point" :filled "true"}
                        :encoding {:x       {:field "effect_size_1",
                                             :type  "quantitative",
                                             :title "Effect Size 1"},
                                   :y       {:field "log_transformed_f_statistic_1",
                                             :type  "quantitative"},
                                   :size {:value 60}
                                   :opacity {:condition {:param "selectedGenes"
                                                         :value 1},
                                             :value     0.2}
                                   :color   {:field "effect_type_1",
                                             :type  "nominal"
                                             :title "Effect type"},
                                   :tooltip [{:field "gene_name_1", :type "nominal", :title "Gene 1"}
                                             {:field "gene_name_2", :type "nominal", :title "Gene 2"}]}}]}
             {:width    700,
              :height   600,
              :mark     {:type "point" :filled "true"}
              :params [{:name   "selectedGenes",
                        :select {:type "interval", :encodings ["x" "y"]}}]
              :encoding {:x       {:field "effect_size_2",
                                   :type  "quantitative",
                                   :title "Effect Size 2"},
                         :y       {:field "log_transformed_f_statistic_2",
                                   :type  "quantitative"},
                         :size {:value 60}
                         :opacity {:condition {:param "selectedGenes"
                                               :value 1},
                                   :value     0.2}
                         :color    {:condition {:param "selectedGenes"
                                                :value "red"}
                                    :field     "effect_type_2"
                                    :type      "nominal"
                                    :title     "Effect type"}
                         #_{:field "effect_type_1",
                            :type  "nominal"
                            :title "Effect type"},
                         :tooltip [{:field "gene_name_1", :type "nominal", :title "Gene 1"}
                                   {:field "gene_name_2", :type "nominal", :title "Gene 2"}]}}
             {:width    400,
              :height   100,
              :mark     "text",
              :encoding {:text {:signal "selectedGenes && selectedGenes.length > 0 ? 'Selected Genes: ' + selectedGenes.map(d => d.gene_name).join(', ') : 'No genes selected'"}}}]})

(defn two-volcanoes-cross-plot
  [cross-data {:keys [x-label
                      y-label
                      width
                      height]}]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data     {:values cross-data}
   :params   [{:name  "p_fdr_threshold",
               :value 0.05,
               :bind  {:input "range", :min 0, :max 1, :step 0.01}}
              {:name   "grid",
               :select "interval",
               :bind   "scales"}]
   :vconcat
   [{:width width,
     :height 80,
     :transform [{:filter "datum.fdr_1 <= p_fdr_threshold && datum.fdr_2 <= p_fdr_threshold"}]
     :mark   {:type "bar" :color "green"}
     :encoding {:x {:field "effect_size_1",
                    :bin   {:maxbins 400},
                    :type  "quantitative",
                    :axis  nil},
                :y {:aggregate "count", :type "quantitative"}}}
    {:hconcat [{:width width,
                :height height,
                :params   [{:name   "p_effect_type_1",
                            :select {:type "point", :fields ["effect_type_1"]},
                            :bind   "legend"}
                           {:name   "p_effect_type_2",
                            :select {:type "point", :fields ["effect_type_2"]},
                            :bind   "legend"}],
                :mark     {:type "point" :tooltip {:content "data"}, :filled "true"}
                :encoding {:x       {:field "effect_size_1",
                                     :type  "quantitative",
                                     :title x-label},
                           :y       {:field "effect_size_2",
                                     :type  "quantitative",
                                     :title y-label},
                           :size {:value 60}
                           :color   {:condition
                                     [{:test  "datum.effect_size_1 >= datum.effect_size_2 + 1",
                                       :value "purple"},
                                      {:test  "datum.effect_size_2 >= datum.effect_size_1 + 1",
                                       :value "green"}],
                                     :value "gray"},
                           :opacity {:condition
                                     {:test
                                      "datum.fdr_1 > p_fdr_threshold || datum.fdr_2 > p_fdr_threshold",
                                      :value 0.2},
                                     :value 1},
                           #_:tooltip #_[{:field "gene_name_1", :title "Gene Name 1", :type "nominal"}
                                         {:field "gene_name_2", :title "Gene Name 2", :type "nominal"}]

                           :href    {:field "url", :type "nominal"}}}
               {:width    80,
                :height height,
                :transform [{:filter "datum.fdr_1 <= p_fdr_threshold && datum.fdr_2 <= p_fdr_threshold"}]
                :mark     {:type "bar" :color "purple"}
                :encoding {:x {:aggregate "count", :type "quantitative"}
                           :y {:field "effect_size_2",
                               :bin   {:maxbins 400},
                               :type  "quantitative",
                               :axis  nil}}}]}]})
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


(defn cross-viz
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

(defn volcano-header []
  [re-com/title
   :src   (at)
   :label "Proteomics Hits Viewer"
   :level :level1])

(defn volcano-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [#_[vega/vega-chart vega/spec]
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
              [:p "hi"]]])

(defmethod routing/panels :volcano [] [volcano-panel])
(defmethod routing/header :volcano [] [volcano-header])
