(ns pipeline.visualization
  (:require
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot]
   [csv-utils :as csv]
   [data-cleaning :as clean]
   [oz.core :as oz]
   [clojure.set :as set]
   [plots.volcanoes :as volcano]
   [math :as math]
   [clojure.string :as str]))

(defn cross-data
  [data-set-1 data-set-2]
  (set/join
   (->> data-set-1
        (map #(set/rename-keys
               %
               {:effect_size                 :effect_size_1
                :effect_type                 :effect_type_1
                :log_transformed_f_statistic :log_transformed_f_statistic_1
                :fdr                         :fdr_1})))
   (->> data-set-2
        (map
         #(set/rename-keys
           %
           {:effect_size                 :effect_size_2
            :effect_type                 :effect_type_2
            :log_transformed_f_statistic :log_transformed_f_statistic_2
            :fdr                         :fdr_2})))))

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

(defn decorate-data
  [data]
  (->> data
       #_(filter (go-term-filtering-fn proteome "GO:0009252"))
       (map
        #(assoc % :url
                (some->> (:uniprot-id %)
                         (str "https://www.uniprot.org/uniprotkb/"))))))

(defn cross-viz
  [data-set-1 data-set-2 {:keys [title
                                 cross-plot-params]}]
  (let [data-set-1 (decorate-data data-set-1)
        data-set-2 (decorate-data data-set-2)
        cross-data (decorate-data (cross-data
                                   data-set-1
                                   data-set-2))]
    [:div
     [:h3 title]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano/standard data-set-1)]
      [:vega-lite (volcano/standard data-set-2)]]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano/cross-highlighting cross-data)]]
     [:div {:style {:display        "flex"
                    :flex-direction "row"}}
      [:vega-lite (volcano/two-volcanoes-cross-plot cross-data cross-plot-params)]]]))





(comment
  (def cefotaxime-ecoli
    (->> (csv/read-csv-data "resources/tpp-cefotaxime-ecoli.csv")
         (map clean/numerify)))

  (def amikacin-ecoli
    (->> (csv/read-csv-data "resources/tpp-amikacin-ecoli.csv")
         (map clean/numerify)))

  (def cefotaxime-pae
    (->> (csv/read-csv-data "resources/tpp-cefotaxime-pae.csv")
         (map clean/numerify)))

  (def amikacin-pae
    (->> (csv/read-csv-data "resources/tpp-amikacin-pae.csv")
         (map clean/numerify)))

  (defonce ecoli-proteome
    (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))

  (defonce pau-proteome
    (api.uniprot/uniprotkb-stream {:query "taxonomy_id:208963"}))

  (defn distrustful-proteome
    [data-set]
    (->> data-set
         (mapcat (comp #(str/split % #"\|") :protein_id))
         (mapv api.uniprot/uniprotkb-entry)
         (filter #(not= "Inactive" (:entryType %)))))

  (do
    (defonce distrustful-ecoli-amikacin (distrustful-proteome amikacin-ecoli))
    (defonce distrustful-ecoli-cefotaxime (distrustful-proteome cefotaxime-ecoli))
    (defonce distrustful-pae-amikacin (distrustful-proteome amikacin-pae))
    (defonce distrustful-pae-cefotaxime (distrustful-proteome cefotaxime-pae)))
 
  ;; schön: die IDs in deren Daten sind wenigstens korrekt gemapped
  (->> distrustful-pae-amikacin
       (map (comp :taxonId :organism))
       distinct)
  
  ;; auch schön: darauf achten "Inactive" records rauszufiltern,
  ;; aber danach ist das hier leer
  (set/difference
   (set (map :primaryAccession distrustful-ecoli-cefotaxime))
   (set (map :primaryAccession ecoli-proteome)))

  (defn go-term-map-stats
    [go-term-map]
    (->> (for [[type terms] go-term-map]
           (let [freqs (vals (frequencies terms))]
             [type {:count            (count terms)
                    :mean-frequency   (int (math/mean freqs))
                    :median-frequency (int (math/median freqs))
                    :max-frequency (apply max freqs)}]))
         (into {})))

  (let [proteome-1 ecoli-proteome
        proteome-2 pau-proteome
        go-terms-1 (uniprot/go-terms-in-proteome proteome-1)
        go-terms-2 (uniprot/go-terms-in-proteome proteome-2)]
    {:proteome-1 (go-term-map-stats go-terms-1)
     :proteome-2 (go-term-map-stats go-terms-2)})

  (oz/start-server!)

  (oz/view! (cross-viz
             cefotaxime-ecoli
             amikacin-ecoli
             {:title "Cefotaxime vs. Amikacin E.Coli"
              :cross-plot-params
              {:x-label "Cefotaxime Fold Change"
               :y-label "Amikacin Fold Change"
               :width   800
               :height  600}}))

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

  {:width     80,
   :height    400,
   :transform [{:density "effect_size_2",
                :as      ["value", "density"]}]

   :mark     "line",
   :encoding {:y {:field "value",
                  :type  "quantitative",
                  :axis  nil},
              :x {:field "density",
                  :type  "quantitative"
                  :sort  "-y"}}}

  (def viz
    (let [data (decorate-data cefotaxime-ecoli)]
      [:div
       [:h3 "TPP Cefotaxime E.Coli"]
       [:div {:style {:display        "flex"
                      :flex-direction "row"}}
        [:vega-lite (volcano/standard data)]]])))


