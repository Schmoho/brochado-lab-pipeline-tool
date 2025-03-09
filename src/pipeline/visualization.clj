(ns pipeline.visualization
  (:require
   [utils :as utils]
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot]
   [biodb.uniprot.mapping :as uniprot.mapping]
   [csv-utils :as csv]
   [data-cleaning :as clean]
   [oz.core :as oz]
   [clojure.set :as set]
   [plots.volcanoes :as volcano]
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

(defn cross-species-data
  [{:keys [data-set-1
           uniprot-proteome-1
           kegg-proteome-1
           data-set-2
           uniprot-proteome-2
           kegg-proteome-2]}]
  (let [mapping-rel   (uniprot.mapping/uniprot-kegg-id-mapping-rel
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
  (do
    (def cefotaxime-ecoli
      (->> (csv/read-csv-data "resources/tpp-cefotaxime-ecoli.csv")
           (mapv clean/numerify)))
    (def amikacin-ecoli
      (->> (csv/read-csv-data "resources/tpp-amikacin-ecoli.csv")
           (mapv clean/numerify)))
    (def cefotaxime-pseudo
      (->> (csv/read-csv-data "resources/tpp-cefotaxime-pae.csv")
           (mapv clean/numerify)))
    (def amikacin-pseudo
      (->> (csv/read-csv-data "resources/tpp-amikacin-pae.csv")
           (mapv clean/numerify)))
    (defonce uniprot-proteome-ecoli
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))
    (defonce uniprot-proteome-pseudo
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:208963"}))
    (def kegg-proteome-ecoli
      (utils/read-file "kegg-83333.edn"))
    (def kegg-proteome-pseudo
      (utils/read-file "kegg-208963.edn")))

  (->> uniprot-proteome-pseudo
       (utils/write!
        "data/raw/uniprot/proteome/208963.edn"))
  

  (cross-data
   cefotaxime-ecoli
   amikacin-ecoli)

  (cross-species-data
   {:data-set-1         cefotaxime-ecoli
    :uniprot-proteome-1 uniprot-proteome-ecoli
    :kegg-proteome-1    kegg-proteome-ecoli
    :data-set-2         cefotaxime-pseudo
    :uniprot-proteome-2 uniprot-proteome-pseudo
    :kegg-proteome-2    kegg-proteome-pseudo})

  (oz/start-server!)

  (oz/view! (cross-viz
             cefotaxime-ecoli
             amikacin-ecoli
             (cross-data cefotaxime-ecoli
                         cefotaxime-pseudo)
             {:title "Cefotaxime vs. Amikacin E.Coli"
              :cross-plot-params
              {:x-label "Cefotaxime Fold Change"
               :y-label "Amikacin Fold Change"
               :width   800
               :height  600}}))

  (oz/view! (cross-viz
             cefotaxime-ecoli
             cefotaxime-pseudo
             (cross-species-data
              {:data-set-1         cefotaxime-ecoli
               :uniprot-proteome-1 uniprot-proteome-ecoli
               :kegg-proteome-1    kegg-proteome-ecoli
               :data-set-2         cefotaxime-pseudo
               :uniprot-proteome-2 uniprot-proteome-pseudo
               :kegg-proteome-2    kegg-proteome-pseudo})
             {:title "Cefotaxime E.Coli vs. P.Aeruginosa"
              :cross-plot-params
              {:x-label "E.Coli Fold Change"
               :y-label "P.Aeruginosa Fold Change"
               :width   800
               :height  600}}))

  (oz/export! [:div] "test.html")

  (def viz
    (let [data (decorate-data cefotaxime-ecoli)]
      [:div
       [:h3 "TPP Cefotaxime E.Coli"]
       [:div {:style {:display        "flex"
                      :flex-direction "row"}}
        [:vega-lite (volcano/standard data)]]]))

  (defn distrustful-proteome
    [data-set]
    (->> data-set
         (mapcat (comp #(str/split % #"\|") :protein_id))
         (mapv api.uniprot/uniprotkb-entry)
         (filter #(not= "Inactive" (:entryType %)))))

  (do
    (defonce distrustful-ecoli-amikacin (distrustful-proteome amikacin-ecoli))
    (defonce distrustful-ecoli-cefotaxime (distrustful-proteome cefotaxime-ecoli))
    (defonce distrustful-pseudo-amikacin (distrustful-proteome amikacin-pseudo))
    (defonce distrustful-pseudo-cefotaxime (distrustful-proteome cefotaxime-pseudo)))

;; schön: die IDs in deren Daten sind wenigstens korrekt gemapped
  (->> distrustful-pseudo-amikacin
       (map (comp :taxonId :organism))
       distinct)

  ;; auch schön: darauf achten "Inactive" records rauszufiltern,
  ;; aber danach ist das hier leer
  (set/difference
   (set (map :primaryAccession distrustful-ecoli-cefotaxime))
   (set (map :primaryAccession uniprot-proteome-ecoli))))


