(ns uniprot.core
  (:require [clojure.string :as str]))

(defn active-sites
  [uniprot-record]
  (let [uniprot-record (if (map? uniprot-record)
                         uniprot-record
                         (second uniprot-record))]
    (->> uniprot-record
         :features
         (filter #(#{"Active site"} (:type %))))))

(defn transpeptidase-sites
  [record]
  (->> (active-sites record)
       (filter #(str/includes? (:description %) "transpeptidase"))))

(defn known-transpeptidase-sites-for-uniprot-records
  [uniprot-records]
  (->> uniprot-records
       (map (juxt first transpeptidase-sites))
       (filter (comp not-empty second))
       (into {})))

(defn has-transpeptidase-site?
  [uniprot-record]
  (-> uniprot-record
      transpeptidase-sites
      not-empty))

(defn inactive-entry?
  [[accession-number entry]]
  (= "Inactive" (:entryType entry)))

(def protein-sequence
  (comp :value :sequence #(if (map? %) % (second %))))

(def organism
  (comp #(select-keys % [:scientificName :taxonId]) :organism #(if (map? %) % (second %))))

(defn distinct-sequences-count
  [uniprot-proteins]
  (count (distinct (map (comp :value :sequence second) uniprot-proteins))))

(defn organism-counts
  [uniprot-proteins]
  (let [grouped (group-by organism uniprot-proteins)]
    (->> grouped
         (map (fn [[k v]]
                [k (count v)]))
         (into {})
         (sort-by (comp :scientificName first)))))

(defn transpeptidase-residue-index
  [site-map id]
  (let [site (get site-map id)]
    (-> site first :location :start :value)))

(defn represented-values
  [accessor uniprot-proteins]
  (keys (group-by (comp accessor val) uniprot-proteins)))

(defn bin-by-length
  [strings step-size]
  (let [lengths   (map count strings) 
        min-len   (apply min lengths)
        max-len   (apply max lengths)
        bins      (range min-len (+ max-len step-size) step-size)
        bin-label (fn [l]
                    (* step-size (int (Math/floor (/ l step-size)))))]

    (reduce (fn [acc s]
              (let [length (count s)
                    bin    (bin-label length)]
                (update acc bin (fnil inc 0))))
            (sorted-map)
            strings)))

(defn sequence-length-overview
  [uniprot-proteins number-of-bins]
  (let [seqs       (keys (group-by protein-sequence uniprot-proteins))
        counts     (map count seqs)
        min-length (apply min counts)
        max-length (apply max counts)]
    {:sequence-length-range     [min-length max-length]
     :sequence-length-histogram (bin-by-length seqs (int (/ (- max-length min-length) number-of-bins)))}))

(defn count-by
  [accessor uniprot-proteins]
  (->> uniprot-proteins
       (group-by (comp accessor val))
       (map (fn [[k v]] [k (count v)]))
       (into {})))

(defn map-proteins
  [fn uniprot-proteins]
  (zipmap
   (keys uniprot-proteins)
   (map fn (vals uniprot-proteins))))


;; (->> idmapping-result
;;      (vals)
;;      (map (comp frequencies (partial map :type) :features))
;;      #_(map #(count-by :type %)))

(defn protein-set-description
  [uniprot-proteins]
  (into (sorted-map)
        [{:total-number                                       (->> uniprot-proteins count)
          :has-transpeptidase-annotation                      (->> uniprot-proteins (filter has-transpeptidase-site?) count)
          :number-of-organisms                                (->> uniprot-proteins (group-by organism) count)
          :number-of-organisms-with-transpeptidase-annotation (->> uniprot-proteins (filter has-transpeptidase-site?) (group-by organism) count)
          :represented-lineages                               (->> uniprot-proteins (represented-values (comp last :lineage :organism)))
          :protein-evidence                                   (->> uniprot-proteins (count-by :proteinExistence))
          :entry-type                                         (->> uniprot-proteins (count-by :entryType))
          :distinct-sequences                                 (->> uniprot-proteins distinct-sequences-count)
          :distinct-sequences-with-transpeptidase-annotation  (->> uniprot-proteins (filter has-transpeptidase-site?) distinct-sequences-count)}
          (sequence-length-overview uniprot-proteins 20)]))




(defn parse-uniprot-records-json
  [filename]
  (let [parsed (json/parse-stream (io/reader filename) true)]
    (zipmap (map name (keys parsed))
            (vals parsed))))

(defn parse-idmapping-result-json
  [filename]
  (let [parsed (:results (json/parse-stream (io/reader filename) true))]
    (zipmap (map :from parsed)
            (map :to parsed))))

(def idmapping-result
  (parse-idmapping-result-json "idmapping_2024_10_23.json"))

(let [seqs (take 2 (map-proteins (juxt (comp :value :sequence)
                                       organism)
                                 idmapping-result))]
  (for [[accession [protein-seq {:keys [scientificName taxonId]}]] seqs]
    [(str ">UniprotAccession: " accession " UniprotTaxon: " taxonId " - " scientificName)
     protein-seq]))

(map-proteins (comp (partial map :referencePositions)
                    :references)
              idmapping-result)

(->>  idmapping-result
      (map (fn [[k v]]
             [k (:references v)]))
      (map (juxt first
                 (comp (partial filter (comp (complement #(str/includes? % "NUCLEOTIDE SEQUENCE"))
                            :referencePositions))
                    second)))
      (filter (comp not-empty second)))

(def doi-getter
  (comp
   (partial map
            (comp (partial map :id)
                  (partial filter #(= "DOI" (:database %)))
                  :citationCrossReferences
                  :citation))
   :references
   val))

(distinct (flatten (map   idmapping-result)))
#_(def blast-uniprotkb-records (uniprot-results-from-blast-result blast-result))
#_(spit "blast-uniprotkb-records.json" (json/generate-string blast-uniprotkb-records))
#_(def blast-uniprotkb-records (parse-uniprot-records-json "blast-uniprotkb-records.json"))
#_(count (filter (complement inactive-entry?) blast-uniprotkb-records)) ;; => 90



#_(def blast-uniparc-records (get-uniparc-results-for-invalid-uniprot-entries! blast-uniprotkb-records))
#_(spit "blast-uniparc-records.json" (json/generate-string blast-uniparc-records))
#_(def blast-uniparc-records (parse-uniprot-records-json "blast-uniparc-records.json"))
#_(count (filter (complement inactive-entry?) blast-uniparc-records)) ;; => 97

#_(def blast-records (merge blast-uniparc-records blast-uniprotkb-records))
#_(distinct-sequences-count blast-records) ;; => 48

(def structures
  (get-structures-for-uniprot-records! blast-records))

(def known-sites-prots
  (known-transpeptidase-sites-for-uniprot-records (merge uniparc-results uniprot-results)))

(def seq-90-structures
  (get-structures-for-uniprot-records! seq-90-percent-sim))

(def seq-90-known-sites
  (known-transpeptidase-sites-for-uniprot-records seq-90-percent-sim))


#_(transpeptidase-sites (first (filter (complement inactive-entry?) uniprot-results))
                        )
