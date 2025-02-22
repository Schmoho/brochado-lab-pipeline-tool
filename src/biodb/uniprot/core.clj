(ns biodb.uniprot.core
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [formats.fasta :refer [->fasta]]
            [clojure.edn :as edn]))

(defmethod ->fasta 
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [protein]
  (let [header
        (str ">"
             ({"UniProtKB unreviewed (TrEMBL)"   "tr|"
               "UniProtKB reviewed (Swiss-Prot)" "sp|"} (:entryType protein))
             (str (:primaryAccession protein) "|")
             (str/join " "
                       [(:uniProtkbId protein)
                        (-> protein :proteinDescription :recommendedName :fullName :value)
                        (str "OS=" (-> protein :organism :scientificName))
                        (str "OX=" (-> protein :organism :taxonId))
                        (str "PE=" (-> protein :proteinExistence (str/split #":" 2) first))
                        (str "GN=" (-> protein :genes first :geneName :value))
                        (str "SV=" (-> protein :entryAudit :sequenceVersion))]))]
    {:fasta/header header
     :fasta/sequence (-> protein :sequence :value)}))

(defmethod ->fasta 
  {:biodb/source :uniprot
   :uniprot/type :uniparc-entry}
  [protein]
  (let [header (str ">"
                    (str/join
                     " "
                     [(-> protein :uniParcId)
                      (str "status=" (if (some :active (:uniParcCrossReferences protein)) "active" "inactive"))
                      (str/join " "
                                (for [{:keys [database id]} (filter :active (:uniParcCrossReferences protein))]
                                  (str database "=" id)))
                      (str "OS="  (->> (:uniParcCrossReferences protein)
                                       (filter :active)
                                       (map (comp :scientificName :organism))
                                       (filter some?)
                                       first))
                      (str "OX="  (->> (:uniParcCrossReferences protein)
                                       (filter :active)
                                       (map (comp :taxonId :organism))
                                       (filter some?)
                                       first))]))]
    {:fasta/header header
     :fasta/sequence (-> protein :sequence :value)}))

(defmulti domains (fn [_ protein] (meta protein)))

(defmethod domains
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [domain-name protein]
  (->> protein
       :features
       (filter (fn [{:keys [type description]}]
                 (and (= "Domain" type)
                      (str/includes? (str/lower-case description)
                                     domain-name))))))

(defmethod domains
  {:biodb/source :uniprot
   :uniprot/type :uniparc-entry}
  [domain-name protein]
  (->> protein
       :sequenceFeatures
       (filter (fn [{:keys [interproGroup locations]}]
                 (some-> interproGroup
                         :name
                         str/lower-case
                         (str/includes? domain-name))))))

(defn restricted-sequence
  [feature protein]
  (subs (-> protein :sequence :value)
         (-> feature :location :start :value)
         (-> feature :location :end :value)))

(defmulti domain-restricted-protein (fn [_ protein] (meta protein)))

(defmethod domain-restricted-protein
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [domain protein]
  (let [domains (domains domain protein)]
    (when (not-empty domains)
      (update-in protein [:sequence :value]
                 (fn [s]
                   (subs s
                         (apply min (map (comp :value :start :location) domains))
                         (apply max (map (comp :value :end :location) domains))))))))

(defmethod domain-restricted-protein
  {:biodb/source :uniprot
   :uniprot/type :uniparc-entry}
  [domain protein]
  (let [domains (domains domain protein)]
    (when (not-empty domains)
      (update-in protein [:sequence :value]
                 (fn [s]
                   (subs s
                         (apply min (mapcat (comp (partial map :start) :locations) domains))
                         (apply max (mapcat (comp (partial map :end) :locations) domains))))))))

;; (->fasta
;;  (with-meta
;;    user/uniprot-protein
;;    {:biodb/source :uniprot
;;    :uniprot/type :uniprotkb-entry}))

;; (->fasta
;;  (with-meta
;;    user/uniparc-protein
;;    {:biodb/source :uniprot
;;     :uniprot/type :uniparc-entry}))

;; (defn active-sites
;;   [uniprot-record]
;;   (let [uniprot-record (if (map? uniprot-record)
;;                          uniprot-record
;;                          (second uniprot-record))]
;;     (->> uniprot-record
;;          :features
;;          (filter #(#{"Active site"} (:type %))))))

;; (defn transpeptidase-sites
;;   [record]
;;   (->> (active-sites record)
;;        (filter #(str/includes? (:description %) "transpeptidase"))))

;; (defn known-transpeptidase-sites-for-uniprot-records
;;   [uniprot-records]
;;   (->> uniprot-records
;;        (map (juxt first transpeptidase-sites))
;;        (filter (comp not-empty second))
;;        (into {})))

;; (defn has-transpeptidase-site?
;;   [uniprot-record]
;;   (-> uniprot-record
;;       transpeptidase-sites
;;       not-empty))

;; (defn inactive-entry?
;;   [[accession-number entry]]
;;   (= "Inactive" (:entryType entry)))

;; (def protein-sequence
;;   (comp :value :sequence #(if (map? %) % (second %))))

;; (def organism
;;   (comp #(select-keys % [:scientificName :taxonId]) :organism #(if (map? %) % (second %))))

;; (defn distinct-sequences-count
;;   [uniprot-proteins]
;;   (count (distinct (map (comp :value :sequence second) uniprot-proteins))))

;; (defn organism-counts
;;   [uniprot-proteins]
;;   (let [grouped (group-by organism uniprot-proteins)]
;;     (->> grouped
;;          (map (fn [[k v]]
;;                 [k (count v)]))
;;          (into {})
;;          (sort-by (comp :scientificName first)))))

;; (defn transpeptidase-residue-index
;;   [site-map id]
;;   (let [site (get site-map id)]
;;     (-> site first :location :start :value)))

;; (defn represented-values
;;   [accessor uniprot-proteins]
;;   (keys (group-by (comp accessor val) uniprot-proteins)))

;; (defn bin-by-length
;;   [strings step-size]
;;   (let [lengths   (map count strings) 
;;         min-len   (apply min lengths)
;;         max-len   (apply max lengths)
;;         bins      (range min-len (+ max-len step-size) step-size)
;;         bin-label (fn [l]
;;                     (* step-size (int (Math/floor (/ l step-size)))))]

;;     (reduce (fn [acc s]
;;               (let [length (count s)
;;                     bin    (bin-label length)]
;;                 (update acc bin (fnil inc 0))))
;;             (sorted-map)
;;             strings)))

;; (defn sequence-length-overview
;;   [uniprot-proteins number-of-bins]
;;   (let [seqs       (keys (group-by protein-sequence uniprot-proteins))
;;         counts     (map count seqs)
;;         min-length (apply min counts)
;;         max-length (apply max counts)]
;;     {:sequence-length-range     [min-length max-length]
;;      :sequence-length-histogram (bin-by-length seqs (int (/ (- max-length min-length) number-of-bins)))}))

;; (defn count-by
;;   [accessor uniprot-proteins]
;;   (->> uniprot-proteins
;;        (group-by (comp accessor val))
;;        (map (fn [[k v]] [k (count v)]))
;;        (into {})))

;; (defn map-proteins
;;   [fn uniprot-proteins]
;;   (zipmap
;;    (keys uniprot-proteins)
;;    (map fn (vals uniprot-proteins))))


;; ;; (->> idmapping-result
;; ;;      (vals)
;; ;;      (map (comp frequencies (partial map :type) :features))
;; ;;      #_(map #(count-by :type %)))

;; (defn protein-set-description
;;   [uniprot-proteins]
;;   (into (sorted-map)
;;         [{:total-number                                       (->> uniprot-proteins count)
;;           :has-transpeptidase-annotation                      (->> uniprot-proteins (filter has-transpeptidase-site?) count)
;;           :number-of-organisms                                (->> uniprot-proteins (group-by organism) count)
;;           :number-of-organisms-with-transpeptidase-annotation (->> uniprot-proteins (filter has-transpeptidase-site?) (group-by organism) count)
;;           :represented-lineages                               (->> uniprot-proteins (represented-values (comp last :lineage :organism)))
;;           :protein-evidence                                   (->> uniprot-proteins (count-by :proteinExistence))
;;           :entry-type                                         (->> uniprot-proteins (count-by :entryType))
;;           :distinct-sequences                                 (->> uniprot-proteins distinct-sequences-count)
;;           :distinct-sequences-with-transpeptidase-annotation  (->> uniprot-proteins (filter has-transpeptidase-site?) distinct-sequences-count)}
;;           (sequence-length-overview uniprot-proteins 20)]))



#_(def idmapping-result
  (parse-idmapping-result-json "idmapping_2024_10_23.json"))

#_(let [seqs (take 2 (map-proteins (juxt (comp :value :sequence)
                                       organism)
                                 idmapping-result))]
  (for [[accession [protein-seq {:keys [scientificName taxonId]}]] seqs]
    [(str ">UniprotAccession: " accession " UniprotTaxon: " taxonId " - " scientificName)
     protein-seq]))

#_(map-proteins (comp (partial map :referencePositions)
                    :references)
              idmapping-result)

#_(->>  idmapping-result
      (map (fn [[k v]]
             [k (:references v)]))
      (map (juxt first
                 (comp (partial filter (comp (complement #(str/includes? % "NUCLEOTIDE SEQUENCE"))
                            :referencePositions))
                    second)))
      (filter (comp not-empty second)))

;; (def doi-getter
;;   (comp
;;    (partial map
;;             (comp (partial map :id)
;;                   (partial filter #(= "DOI" (:database %)))
;;                   :citationCrossReferences
;;                   :citation))
;;    :references
;;    val))

#_(distinct (flatten (map   idmapping-result)))
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

#_(def structures
  (get-structures-for-uniprot-records! blast-records))

#_(def known-sites-prots
  (known-transpeptidase-sites-for-uniprot-records (merge uniparc-results uniprot-results)))

#_(def seq-90-structures
  (get-structures-for-uniprot-records! seq-90-percent-sim))

#_(def seq-90-known-sites
  (known-transpeptidase-sites-for-uniprot-records seq-90-percent-sim))


#_(transpeptidase-sites (first (filter (complement inactive-entry?) uniprot-results))
                        )
