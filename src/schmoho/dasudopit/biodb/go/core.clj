(ns schmoho.dasudopit.biodb.go.core)

(def evidence-code-lookup
  {"EXP" "Inferred from Experiment"
   "IDA" "Inferred from Direct Assay"
   "IPI" "Inferred from Physical Interaction"
   "IMP" "Inferred from Mutant Phenotype"
   "IGI" "Inferred from Genetic Interaction"
   "IEP" "Inferred from Expression Pattern"
   "HTP" "Inferred from High Throughput Experiment"
   "HDA" "Inferred from High Throughput Direct Assay"
   "HMP" "Inferred from High Throughput Mutant Phenotype"
   "HGI" "Inferred from High Throughput Genetic Interaction"
   "HEP" "Inferred from High Throughput Expression Pattern"
   "IBA" "Inferred from Biological aspect of Ancestor"
   "IBD" "Inferred from Biological aspect of Descendant"
   "IKR" "Inferred from Key Residues"
   "IRD" "Inferred from Rapid Divergence"
   "ISS" "Inferred from Sequence or structural Similarity"
   "ISO" "Inferred from Sequence Orthology"
   "ISA" "Inferred from Sequence Alignment"
   "ISM" "Inferred from Sequence Model"
   "IGC" "Inferred from Genomic Context"
   "RCA" "Inferred from Reviewed Computational Analysis"
   "TAS" "Traceable Author Statement"
   "NAS" "Non-traceable Author Statement"
   "IC"  "Inferred by Curator"
   "ND"  "No biological Data available"
   "IEA" "Inferred from Electronic Annotation"})


(def evidence-code-lookup
  {"ECO:0000269" "EXP"
   "ECO:0000314" "IDA"
   "IPI" "Inferred from Physical Interaction"
   "IMP" "Inferred from Mutant Phenotype"
   "IGI" "Inferred from Genetic Interaction"
   "IEP" "Inferred from Expression Pattern"
   "HTP" "Inferred from High Throughput Experiment"
   "HDA" "Inferred from High Throughput Direct Assay"
   "HMP" "Inferred from High Throughput Mutant Phenotype"
   "HGI" "Inferred from High Throughput Genetic Interaction"
   "HEP" "Inferred from High Throughput Expression Pattern"
   "IBA" "Inferred from Biological aspect of Ancestor"
   "IBD" "Inferred from Biological aspect of Descendant"
   "IKR" "Inferred from Key Residues"
   "IRD" "Inferred from Rapid Divergence"
   "ISS" "Inferred from Sequence or structural Similarity"
   "ISO" "Inferred from Sequence Orthology"
   "ISA" "Inferred from Sequence Alignment"
   "ISM" "Inferred from Sequence Model"
   "IGC" "Inferred from Genomic Context"
   "RCA" "Inferred from Reviewed Computational Analysis"
   "TAS" "Traceable Author Statement"
   "NAS" "Non-traceable Author Statement"
   "IC"  "Inferred by Curator"
   "ND"  "No biological Data available"
   "IEA" "Inferred from Electronic Annotation"})

(defn count-by
  [m thing]
  (->>
   (for [thing-type (distinct (map thing m))]
     [thing-type
      (count (get (group-by thing m) thing-type))])
   (into {})))

(defn translate-keys
  [m lookup]
  (->> m
       (map (fn [[k v]]
              [(or (lookup k) k) v]))
       (into {})))

(defn mean-of-list-val-counts
  [map-of-lists]
  (/ (->> map-of-lists
         vals
         (map count)
         (reduce +))
     (count map-of-lists)))

(defn median-of-list-val-counts
  [map-of-lists]
  (let [counts (->> map-of-lists
                    vals
                    (map count)
                    (sort)
                    vec)
        ccount (count counts)]
    (if (even? ccount)
      (get counts (+ (get counts (/ ccount 2))
                     (get counts (inc (/ ccount 2)))))
      (get counts (inc (int (/ ccount 2)))))))

;; (defn gaf-stats
;;   [gaf]
;;   (let [proteins->terms (proteins->terms gaf)]
;;     {:count                     (count gaf)
;;      :distinct-terms            (count (distinct (map :go-id gaf)))
;;      :relation-types            (count-by gaf :relation)
;;      :count-by-origin           (count-by gaf :db)
;;      :count-by-evidence-code    (-> gaf
;;                                  (count-by :evidence-code)
;;                                  (translate-keys evidence-code-lookup))
;;      :count-by-aspect           (->>
;;                                  (for [aspect ["F" "C" "P"]]
;;                                    [({"F" :function
;;                                       "C" :compartment
;;                                       "P" :process} aspect)
;;                                  (count (get (group-by :aspect gaf) aspect))])
;;                                  (into {}))
;;      :median-terms-for-proteins (median-of-list-val-counts proteins->terms)
;;      :mean-terms-for-proteins   (int (mean-of-list-val-counts proteins->terms))}))

;; {:pseudocap (gaf-stats pseudocap-gaf)
;;  :uniprot   (gaf-stats pa-gaf)}

;; Wieviele Gene die annotiert sind können orthology-gemapped werden?
;; Wie groß ist der overlap zwischen den orthology-mapped Genen?
;; Dasselbe für Pathways an denen gemappte Gene beteiligt sind

;; (-> @db/db :eco :proteins)
;; ;; => 5771

;; (->> @db/db :eco :annotations (drop 20) first)

(defn orthology-mappable-genes
  [gaf orthology-lookup]
  (->> gaf
       (map :db-object-id)
       distinct
       (filter #(and (orthology-lookup %)
                     (some? %)))))

;; (count (orthology-mappable-genes ec-gaf kegg/ec-orthology-lookup))
;; ;; => 3126
;; (count (orthology-mappable-genes pa-gaf kegg/pa-orthology-lookup))
;; ;; => 3165

;; (let [ec-mapped (->> (map (comp kegg/ec-orthology-lookup :db-object-id) ec-gaf)
;;                      (filter some?)
;;                      set)
;;       pa-mapped (->> (map (comp kegg/pa-orthology-lookup :db-object-id) pa-gaf)
;;                      (filter some?)
;;                      set)]
;;   (count (set/intersection ec-mapped pa-mapped)))
;; ;; => 1584

;; (def go-lookup
;;   (->> go
;;       :graphs
;;       first
;;       :nodes
;;       (map (fn [stuff]
;;              (update stuff :id (fn [id-url]
;;                                  (-> id-url
;;                                      (str/split #"/")
;;                                      last
;;                                      (str/replace "_" ":"))))))
;;       (group-by :id)
;;       (map (fn [[id stuffs]]
;;              [id (first stuffs)]))
;;       (into {})))

(defn represented-go-terms
  [gene-name-lookup hits]
  (->> (map :gene hits)))

;; (defn go-term-frequencies
;;   [hits kegg->uniprot organism-go-terms gene-name-lookup]
;;   (->> hits
;;        (map :gene)
;;        (map (fn [gene]
;;               (let [go-terms (or (-> gene
;;                                      kegg->uniprot
;;                                      organism-go-terms)
;;                                 (->> gene
;;                                      gene-name-lookup
;;                                      (map kegg->uniprot)
;;                                      (map organism-go-terms)))]
;;                go-terms)))
;;       flatten
;;       (map (fn [go-term]
;;              (when go-term
;;                [(:relation go-term) (:go-id go-term)])))
;;       (filter some?)
;;       (frequencies)
;;       (map (fn [[[relation go-term] frequency]]
;;              [[relation
;;                go-term
;;                (-> (go-lookup go-term) :lbl)]
;;               frequency]))
;;       (sort-by (fn [[[relation go-term] frequency]]
;;                  frequency))
;;       reverse
;;       (into {})))


;; (let [pa-freqs (->> (go-term-frequencies excel/pa-amikacin-hits
;;                                          pseudo-kegg->uniprot
;;                                          pseudo-go-terms
;;                                          pseudo-gene-name-lookup)
;;                     (map (fn [[k v]]
;;                            [k {:pa v}]))
;;                     (into {}))
;;       ec-freqs (->> (go-term-frequencies excel/ec-amikacin-hits
;;                                          ec-kegg->uniprot
;;                                          ec-go-terms
;;                                          ec-gene-name-lookup)
;;                     (map (fn [[k v]]
;;                            [k {:ec v}]))
;;                     (into {}))]
;;   (merge-with merge pa-freqs ec-freqs))
