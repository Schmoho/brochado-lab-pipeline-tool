(ns pipeline.visualization
  (:require
   [data-cleaning :as clean]
   [csv-utils :as csv]
   [oz.core :as oz]
   [clojure.string :as str]
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot]))

(def raw
  (->> (csv/read-csv-data "resources/tpp-raw-cefotaxime-ecoli.csv")
       (transduce
        (comp
         (clean/filtering-insanity
          {:protein_id #(str/starts-with? % "#")})
         clean/numerify)
        conj
        [])))

(def processed
  (->> (csv/read-csv-data "resources/tpp-processed-cefotaxime-ecoli.csv")
       (transduce
        (comp
         clean/numerify)
        conj
        [])))

(defonce proteome
  (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))

(defn get-gene-name->protein-id-mapping
  [raw-table]
  (->> raw-table
      (map (juxt :gene_name :protein_id))
      distinct
      (into {})))

(defn with-uniprot-id-fn
  [proteome raw]
  (let [gene-name->protein-id
        (get-gene-name->protein-id-mapping raw)
        proteome-lookup
        (uniprot/proteome-lookup proteome)]
    (fn [table-row]
      (assoc table-row
             :uniprot-id
             (some->> table-row
                      :clustername
                      gene-name->protein-id
                      proteome-lookup
                      :primaryAccession)))))

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
  {:params [{:name  "vertical_threshold",
             :value 0,
             :bind  {:input "range", :min -10, :max 10, :step 0.1}},
            {:name  "horizontal_threshold",
             :value 5,
             :bind  {:input "range", :min 0, :max 10, :step 0.1}}]
   :layer  [{:data      {:values data}
             :params    [{:name   "effect_type",
                          :select {:type "point", :fields ["detected_effectH1"]},
                          :bind   "legend"}]
             :transform [{:calculate "(datum.slopeH1 > 0 ? 1 : -1) * sqrt(datum.rssH0 - datum.rssH1)",
                          :as        "effect_size"},
                         {:calculate "log(datum.F_statistic + 1) / log(2)",
                          :as        "log_transformed_F_statistic"}]
             :encoding  {:x       {:field "effect_size",
                                   :type  "quantitative",
                                   :title "Effect Size"},
                         :y       {:field "log_transformed_F_statistic",
                                   :type  "quantitative",
                                   :title "Log2(F-statistic + 1)"}
                         :color   {:condition {:test  "datum.effect_size > vertical_threshold && datum.log_transformed_F_statistic > horizontal_threshold",
                                               :field "detected_effectH1",
                                               :type  "nominal"
                                               :title "Effect type"},
                                   :value     "grey"}
                         :opacity {:condition {:param "effect_type", :value 1},
                                   :value     0}
                         :tooltip [{:field "clustername", :title "Gene Cluster Name" :type "nominal"}
                                   {:field "effect_size", :title "Fold Change" :type "quantitative"}]
                         :href    {:field "url", :type "nominal"}}
             :mark      #_{:type "point", :tooltip true} "point"}
            #_#_{:mark     {:type        "rule",
                            :strokeDash  [4, 4],
                            :color       "black",
                            :strokeWidth 2},
                 :encoding {:x {:field "vertical_threshold"}}}
              {:mark     {:type         "rule",
                          :strokeDash   [4, 4],
                          :color        "black",
                          "strokeWidth" 2},
               :encoding {:y {:field "horizontal_threshold"}}}

            {:data {:values [{}]},
             :transform [{:calculate "vertical_threshold", :as "vt"}],
             :mark {:type "rule", :strokeDash [4, 4], :color "black", :strokeWidth 2},
             :encoding {:x {:field "vt", :type "quantitative"}}}

            {:data {:values [{}]},
             :transform [{:calculate "horizontal_threshold", :as "ht"}],
             :mark {:type "rule", :strokeDash [4, 4], :color "black", :strokeWidth 2},
             :encoding {:y {:field "ht", :type "quantitative"}}}]
   :width  400,
   :height 400})

(def viz
  (let [data (->> processed
                  (map (with-uniprot-id-fn proteome raw))
                  (filter (go-term-filtering-fn proteome "GO:0009252"))
                  (map (fn [m]
                         (assoc m :url
                                (some->> (:uniprot-id m)
                                         (str "https://www.uniprot.org/uniprotkb/"))))))]
    [:div
     [:h3 "TPP Cefotaxime E.Coli"]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano-plot data)]]]))

#_(oz/view! viz)

#_(oz/start-server!)

(comment

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
     :r_sq])

  (csv-utils/field-domain :protein_id raw))
