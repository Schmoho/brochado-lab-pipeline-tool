(ns pipeline.visualization
  (:require
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot]
   [clojure.string :as str]
   [csv-utils :as csv]
   [data-cleaning :as clean]
   [oz.core :as oz]
   [clojure.set :as set]
   [cheshire.core :as json]))

(def cefotaxime-ecoli
  (->> (csv/read-csv-data "resources/tpp-cefotaxime-ecoli.csv")
       (map clean/numerify)))

(def amikacin-ecoli
  (->> (csv/read-csv-data "resources/tpp-amikacin-ecoli.csv")
       (map clean/numerify)))

(defonce proteome
  (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))



(defn with-uniprot-id-fn
  [id-lookup]
  (fn [table-row]
    (assoc table-row
           :uniprot-id
           (some->> table-row
                    :protein_id
                    id-lookup))))

(defn go-term-filtering-fn
  [proteome go-term]
  (let [proteome-lookup
        (uniprot/proteome-lookup proteome)]
    (fn [table-row]
      (some->> table-row
               :uniprot-id
               proteome-lookup
               (uniprot/has-go-term? go-term)))))


(defn volcano-plot
  [data]
  {:data      {:values data}
   :params    [{:name   "p_effect_type",
                :select {:type "point", :fields ["effect_type"]},
                :bind   "legend"}]
   :encoding  {:x       {:field "effect_size",
                         :type  "quantitative",
                         :title "Effect Size"
                         :scale {:domain [-8, 8],
                                 :nice false}},
               :y       {:field "log_transformed_f_statistic",
                         :type  "quantitative",
                         :title "Log2(F-statistic + 1)"}
               :color   {:field "effect_type",
                         :type  "nominal"
                         :title "Effect type"}
               :opacity {:condition {:param "p_effect_type", :value 1},
                         :value     0}
               :tooltip [{:field "gene_name", :title "Gene Name" :type "nominal"}
                         {:field "effect_size", :title "Fold Change" :type "quantitative"}]
               :href    {:field "url", :type "nominal"}}
   :mark      "point"
   :width     600,
   :height    400})

(def viz
  (let [data (->> cefotaxime-ecoli
                  (map (with-uniprot-id-fn
                         (comp
                          :primaryAccession
                          (uniprot/proteome-lookup proteome))))
                  #_(filter (go-term-filtering-fn proteome "GO:0009252"))
                  (map (fn [m]
                         (assoc m :url
                                (some->> (:uniprot-id m)
                                         (str "https://www.uniprot.org/uniprotkb/"))))))]
    [:div
     [:h3 "TPP Cefotaxime E.Coli"]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano-plot data)]]]))

(def cross-data
  (set/join
  (map
   #(set/rename-keys %
                     {:effect_size :effect_size_1
                      :effect_type :effect_type_1
                      :log_transformed_f_statistic :log_transformed_f_statistic_1
                      :fdr :fdr_1})
   cefotaxime-ecoli)
  (map
   #(set/rename-keys %
                     {:effect_size :effect_size_2
                      :effect_type :effect_type_2
                      :log_transformed_f_statistic :log_transformed_f_statistic_2
                      :fdr :fdr_2})
   amikacin-ecoli)))

(defn volcano-cross-plot
  [cross-data {:keys [x-label
                      y-label]}]
  {:data     {:values cross-data}
   :params   [{:name   "p_effect_type_1",
               :select {:type "point", :fields ["effect_type_1"]},
               :bind   "legend"}
              {:name   "p_effect_type_2",
               :select {:type "point", :fields ["effect_type_2"]},
               :bind   "legend"}
              {:name  "p_fdr_threshold",
               :value 0.05,
               :bind  {:input "range", :min 0, :max 1, :step 0.01}}
              {:name   "grid",
               :select "interval",
               :bind   "scales"}]
   :vconcat
   [{:width  400,
     :height 80,
     :mark   "bar",
     :encoding
     {:x {:field "effect_size_1",
          :bin   {:maxbins 20},
          :type  "quantitative",
          :axis  nil},
      :y {:aggregate "count", :type "quantitative"}}}
    {:hconcat [{:width    400,
                :height   400,
                :params   [{:name   "p_effect_type_1",
                            :select {:type "point", :fields ["effect_type_1"]},
                            :bind   "legend"}
                           {:name   "p_effect_type_2",
                            :select {:type "point", :fields ["effect_type_2"]},
                            :bind   "legend"}],
                :mark     "point",
                :encoding {:x       {:field "effect_size_1",
                                     :type  "quantitative",
                                     :title "Cefotaxime Fold Change"},
                           :y       {:field "effect_size_2",
                                     :type  "quantitative",
                                     :title "Amikacin Fold Change"},
                           :color   {:condition
                                     [{:test  "datum.effect_size_1 >= datum.effect_size_2 + 1",
                                     :value "steelblue"},
                                    {:test  "datum.effect_size_2 >= datum.effect_size_1 + 1",
                                     :value "orange"}],
                                     :value "gray"},
                           :opacity {:condition
                                     {:test
                                      "number(datum.fdr_1) < p_fdr_threshold && number(datum.fdr_2) < p_fdr_threshold",
                                      :value 0.2},
                                     :value 1},
                           :tooltip [{:field "gene_name", :title "Gene Name", :type "nominal"}],
                           :href    {:field "url", :type "nominal"}}}
               {:width    80,
                :height   400,
                :mark     "bar",
                :encoding {:x {:aggregate "count", :type "quantitative"}
                           :y {:field "effect_size_2",
                               :bin   {:maxbins 20},
                               :type  "quantitative",
                               :axis  nil}}}]}]
   :encoding {:x       {:field "effect_size_1",
                        :type  "quantitative",
                        :title x-label}
              :y       {:field "effect_size_2",
                        :type  "quantitative",
                        :title y-label},
              :color   {:condition [{:test  "datum.effect_size_1 >= datum.effect_size_2 + 1",
                                     :value "steelblue"},
                                    {:test  "datum.effect_size_2 >= datum.effect_size_1 + 1",
                                     :value "orange"}]
                        :value     "gray"},
              :opacity {:condition
                        {:test  "datum.fdr_1 > p_fdr_threshold || datum.fdr_2 > p_fdr_threshold",
                         :value 0.2},
                        :value 1},
              :tooltip [{:field "gene_name", :title "Gene Name" :type "nominal"}
                        {:field "fdr_1", :title "FDR Cefotaxime" :type "nominal"}
                        {:field "fdr_2", :title "FDR Amikacin" :type "nominal"}]
              :href    {:field "url", :type "nominal"}}
   :mark     "point"
   :width    700,
   :height   500})

{:params
 [{:name  "p_fdr_threshold",
   :value 0.05,
   :bind  {:input "range", :min 0, :max 1, :step 0.01}}],
 }

(def cross-viz
  (let [cross-data (->> cross-data
                        (filter #(and (pos? (:fdr_1 %))
                                      (pos? (:fdr_2 %))))
                        (map (with-uniprot-id-fn
                               (comp
                                :primaryAccession
                                (uniprot/proteome-lookup proteome))))
                        #_(filter (go-term-filtering-fn proteome "GO:0009252"))
                        (map (fn [m]
                               (assoc m :url
                                      (some->> (:uniprot-id m)
                                               (str "https://www.uniprot.org/uniprotkb/"))))))]
    [:div
     [:h3 "Cefotaxime vs. Amikacin E.Coli"]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano-cross-plot cross-data
                                      {:x-label "Cefotaxime Fold Change"
                                       :y-label "Amikacin Fold Change"})]]]))


(comment
  (oz/start-server!)
  
  (oz/view! viz)

  (oz/view! cross-viz)

  (->> (map uniprot/go-terms-in-protein proteome)
       (apply concat)
       (group-by :type))

  (oz/export! [:div] "test.html")

  #(select-keys
    %
    [:protein_id
     :norm_rel_fc_protein_2_5_transformed
     :norm_rel_fc_protein_10_transformed
     :norm_rel_fc_protein_50_transformed
     :norm_rel_fc_protein_250_transformed
     :temperature
     :pec50
     :slope
     :r_sq]))

