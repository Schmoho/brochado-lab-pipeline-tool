(ns unknown-client.vega-utils
  (:require
   [clojure.set :as set]))

(defn clicked-points-signal
  [signal-value]
  (-> (js->clj signal-value)
      (get "vlPoint")
      (get "or")
      (->> (map (fn [a] (get a "_vgsid_"))))))

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

(defn brushed-points
  [signal table]
  (let [[x-left x-right] (get signal "effect_size")
        [y-left y-right] (get signal "log_transformed_f_statistic")]
    (when (not-empty signal)
      (->> table
           (filter
            (fn [{:keys [effect_size log_transformed_f_statistic]}]
              (and
               (<= x-left effect_size x-right)
               (<= y-left log_transformed_f_statistic y-right))))
           (map #(:gene_name %))))))
