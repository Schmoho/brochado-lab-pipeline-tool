(ns pipeline.visualization
  (:require
   [data-cleaning :as clean]
   [oz.core :as oz]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn csv-lines->maps
  [csv-lines & {:keys [keywordize?] :or {keywordize? true}}]
  (map zipmap
       (->> (first csv-lines)
            (map
             (comp
              (if keywordize?
                keyword
                identity)
              #(str/replace % "." "_")))
            repeat)
       (rest csv-lines)))

(defn field-domain
  [field csv-data]
  (set (map field csv-data)))

(def data
  (->> (io/reader "resources/tpp-raw-cefotaxime-ecoli.csv")
       csv/read-csv
       csv-lines->maps
       (transduce
        (comp
         (clean/filtering-insanity
          {:protein_id #(str/starts-with? % "#")})
         clean/numerify)
        conj
        [])))

(def processed
  (->> (io/reader "resources/tpp-processed-cefotaxime-ecoli.csv")
       csv/read-csv
       csv-lines->maps
       (transduce
        (comp
         #_(clean/filtering-insanity
          {:protein_id #(str/starts-with? % "#")})
         clean/numerify)
        conj
        [])))



(let [data (map #(select-keys
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
                data)
      data-to-write (concat [(map name (keys (first data)))]
                            (map vals data))]
  (with-open [writer (io/writer "out-file.csv")]
    (csv/write-csv writer
                   data-to-write))
  data-to-write)

(field-domain :protein_id data)

(oz/start-server!)

(def line-plot
  {:params [{:name  "vertical_threshold",
             :value 0,
             :bind  {:input "range", :min -10, :max 10, :step 0.1}},
            {:name  "horizontal_threshold",
             :value 5,
             :bind  {:input "range", :min 0, :max 10, :step 0.1}}]
   :layer  [{:data      {:values (->> processed
                                      (map (fn [m]
                                             (assoc m :url (str "https://www.uniprot.org/uniprotkb?query=" (:clustername m)))))
                                      #_(map (fn [m]
                                               (select-keys m [:slopeH1 :rssH0 :rssH1 :F_statistic]))))}
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

(oz/view! line-plot)

(def viz
  [:div
   [:h3 "TPP Cefotaxime E.Coli"]
   [:div {:style {:display        "flex"
                  :flex-direction "row"}}
    [:vega-lite line-plot]]])

(oz/view! viz)

(oz/export! viz "test.html")

