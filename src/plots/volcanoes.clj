(ns plots.volcanoes)

(defn standard
  [data]
  {:data      {:values data}
   :params    [{:name   "p_effect_type",
                :select {:type "point", :fields ["effect_type"]},
                :bind   "legend"}
               {:name  "p_fdr_threshold",
                :value 0.05,
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
   :mark      "point"
   :width     600,
   :height    400})

(defn cross-highlighting
  [data]
  {:data     {:values data}
   :hconcat [{:width    700,
              :height   600,
              :layer [{:params   [{:name   "selectedGenes",
                          :select {:type "interval", :encodings ["x" "y"]}}
                         {:name   "p_effect_type",
                          :select {:type "point", :fields ["effect_type"]},
                          :bind   "legend"}
                         {:name  "p_fdr_threshold",
                          :value 0.05,
                          :bind  {:input "range", :min 0, :max 1, :step 0.01}}
                         #_{:name   "grid",
                            :select "interval",
                            :bind   "scales"}],
              :mark     "point",
              :encoding {:x       {:field "effect_size_1",
                                   :type  "quantitative",
                                   :title "Effect Size 1"},
                         :y       {:field "log_transformed_f_statistic_1",
                                   :type  "quantitative"},
                         :opacity {:condition {:param "selectedGenes"
                                               :value 1},
                                   :value     0.2}
                         :color   {:field "effect_type_1",
                                   :type  "nominal"
                                   :title "Effect type"},
                         :tooltip [{:field "gene_name", :type "nominal", :title "Gene"}]}}]}
             {:width    700,
              :height   600,
              :mark     "point",
              :encoding {:x       {:field "effect_size_2",
                                   :type  "quantitative",
                                   :title "Effect Size 2"},
                         :y       {:field "log_transformed_f_statistic_2",
                                   :type  "quantitative"},
                         :opacity {:condition {:param "selectedGenes"
                                               :value 1},
                                   :value     0.2}
                         :color   {:field "effect_type_1",
                                   :type  "nominal"
                                   :title "Effect type"},
                         :tooltip [{:field "gene_name", :type "nominal", :title "Gene"}]}}
             {:width 400,
              :height 100,
              :mark "text",
              :encoding {:text {:signal "selectedGenes && selectedGenes.length > 0 ? 'Selected Genes: ' + selectedGenes.map(d => d.gene_name).join(', ') : 'No genes selected'"}}}]})

(defn two-volcanoes-cross-plot
  [cross-data {:keys [x-label
                      y-label
                      width
                      height]}]
  {:data     {:values cross-data}
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
     :mark   {:type "bar" :color "orange"}
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
                :mark     "point",
                :encoding {:x       {:field "effect_size_1",
                                     :type  "quantitative",
                                     :title x-label},
                           :y       {:field "effect_size_2",
                                     :type  "quantitative",
                                     :title y-label},
                           :color   {:condition
                                     [{:test  "datum.effect_size_1 >= datum.effect_size_2 + 1",
                                       :value "steelblue"},
                                      {:test  "datum.effect_size_2 >= datum.effect_size_1 + 1",
                                       :value "orange"}],
                                     :value "gray"},
                           :opacity {:condition
                                     {:test
                                      "datum.fdr_1 > p_fdr_threshold || datum.fdr_2 > p_fdr_threshold",
                                      :value 0.2},
                                     :value 1},
                           :tooltip [{:field "gene_name", :title "Gene Name", :type "nominal"}],
                           :href    {:field "url", :type "nominal"}}}
               {:width    80,
                :height height,
                :transform [{:filter "datum.fdr_1 <= p_fdr_threshold && datum.fdr_2 <= p_fdr_threshold"}]
                :mark     {:type "bar" :color "steelblue"}
                :encoding {:x {:aggregate "count", :type "quantitative"}
                           :y {:field "effect_size_2",
                               :bin   {:maxbins 400},
                               :type  "quantitative",
                               :axis  nil}}}]}]})
