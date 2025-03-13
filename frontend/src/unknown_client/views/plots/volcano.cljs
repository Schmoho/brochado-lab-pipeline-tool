(ns unknown-client.views.plots.volcano)

(defn single-pan
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
                :bind   "scales"}
               {:name   "pick",
                :select {:type "point" :on "click"}}
               {:name   "boing",
                :bind {:element "#test"}}]
   :encoding  {:x       {:field "effect_size",
                         :type  "quantitative",
                         :title "Effect Size"},
               :y       {:field "log_transformed_f_statistic",
                         :type  "quantitative",
                         :title "Log2(F-statistic + 1)"}
               :size {:value 60}
               :color   {:condition {:param "pick"
                                      :field "effect_type",
                                      :type  "nominal"
                                      :title "Effect type"}
                         :value "grey"}
               :opacity {:condition [{:test "(datum.fdr <= p_fdr_threshold)",
                                      :value 1}
                                     #_{:param "p_effect_type", :value 1}]
                         :value 0.2}
               :tooltip [{:field "gene_name", :title "Gene Name" :type "nominal"}
                         {:field "effect_size", :title "Fold Change" :type "quantitative"}]
               #_#_:href    {:field "url", :type "nominal"}}
   :mark      {:type "point" :filled "true"}
   :width     600,
   :height    400})


(defn single-pan-searchable
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
                :bind   "scales"}
               {:name   "pick",
                :select {:type "point" :on "click"}}
               {:name   "boing",
                :bind {:element "#test"}}]
   :encoding  {:x       {:field "effect_size",
                         :type  "quantitative",
                         :title "Effect Size"},
               :y       {:field "log_transformed_f_statistic",
                         :type  "quantitative",
                         :title "Log2(F-statistic + 1)"}
               :size {:value 60}
               :color   {:condition {:test "datum.gene_name === boing",
                                      :field "effect_type",
                                      :type  "nominal"
                                      :title "Effect type"}
                         :value "grey"}
               :opacity {:condition [{:test "(datum.fdr <= p_fdr_threshold)",
                                      :value 1}
                                     #_{:param "p_effect_type", :value 1}]
                         :value 0.2}
               :tooltip [{:field "gene_name", :title "Gene Name" :type "nominal"}
                         {:field "effect_size", :title "Fold Change" :type "quantitative"}]
               #_#_:href    {:field "url", :type "nominal"}}
   :mark      {:type "point" :filled "true"}
   :width     600,
   :height    400})

(defn single-brush
  [data]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data    {:values data}
   :params  [{:name  "p_fdr_threshold",
              :value 0.10,
              :bind  {:input "range", :min 0, :max 1, :step 0.01}}]
   :layer   [{:mark   {:type "point" :filled "true"}
              :params [{:name   "p_effect_type",
                        :select {:type "point", :fields ["effect_type"]},
                        :bind   "legend"}

                       {:name   "brush",
                        :select "interval"
                        :empty  false}
                       #_{:name   "brush",
                          :select "point"}]
              :encoding {:x         {:field "effect_size",
                                     :type  "quantitative",
                                     :title "Effect Size"},
                         :y         {:field "log_transformed_f_statistic",
                                     :type  "quantitative",
                                     :title "Log2(F-statistic + 1)"}
                         :size      {:value 60}
                         #_#_:color {:field "effect_type",
                                     :type  "nominal"
                                     :title "Effect type"}
                         :color     {:condition {:param "brush"
                                                 :value "green"
                                                 :empty false}
                                     :value     "blue"}

                         :opacity  {:condition [{:test  "(datum.fdr <= p_fdr_threshold)",
                                                 :value 1}
                                                #_{:param "p_effect_type", :value 1}]
                                    :value     0.2}
                         :tooltip  [{:field "gene_name", :title "Gene Name" :type "nominal"}
                                    {:field "effect_size", :title "Fold Change" :type "quantitative"}]
                         #_#_:href {:field "url", :type "nominal"}}}
             {:mark     {:type "text"}
              :encoding {:text    {:field "gene_name"}
                         :opacity {:condition [{:param "brush"
                                                :value 1
                                                :empty false}
                                               #_{:param "p_effect_type", :value 1}]
                                   :value     0}
                         :x       {:field "effect_size",
                                   :type  "quantitative",
                                   :title "Effect Size"},
                         :y       {:field "log_transformed_f_statistic",
                                   :type  "quantitative",
                                   :title "Log2(F-statistic + 1)"}}}]
   :width  600,
   :height 400})

#_:text #_{:condition
                        {:test "datum['category'] === 'A'"
                         :field "gene_name",
                         :type "nominal"},
                        :value "Not A"}
               #_{#_#_:condition
                  {:test "brush"
                   :field "gene_name"
                   :type "nominal"}
                :field "gene_name"
                :type "nominal"}

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
  [cross-data
   {:keys [width height x-label y-label]}]
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
                           #_#_:href    {:field "url", :type "nominal"}}}
               {:width    80,
                :height height,
                :transform [{:filter "datum.fdr_1 <= p_fdr_threshold && datum.fdr_2 <= p_fdr_threshold"}]
                :mark     {:type "bar" :color "purple"}
                :encoding {:x {:aggregate "count", :type "quantitative"}
                           :y {:field "effect_size_2",
                               :bin   {:maxbins 400},
                               :type  "quantitative",
                               :axis  nil}}}]}]})
