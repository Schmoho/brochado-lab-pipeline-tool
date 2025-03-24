(ns schmoho.dasudopit.biodb.uniprot.core
  (:require
   [clojure.string :as str]
   [schmoho.dasudopit.formats.fasta :refer [->fasta]]
   [schmoho.dasudopit.utils :as utils]))

(defmethod ->fasta 
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [protein]
  (let [header
        (str ">"
             ({"UniProtKB unreviewed (TrEMBL)"   "tr|"
               "UniProtKB reviewed (Swiss-Prot)" "sp|"} (:entryType protein))
             (:primaryAccession protein)
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
                                     (str/lower-case domain-name)))))))

(defmethod domains
  {:biodb/source :uniprot
   :uniprot/type :uniparc-entry}
  [domain-name protein]
  (->> protein
       :sequenceFeatures
       (filter (fn [{:keys [interproGroup]}]
                 (some-> interproGroup
                         :name
                         str/lower-case
                         (str/includes? (str/lower-case domain-name)))))))

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


(defmulti active-sites
  "Returns protein feature maps that represent an active site with the given name.
  Dispatches on protein metadata."
  (fn [_ protein] (meta protein)))

(defmethod active-sites
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry}
  [active-site-name protein]
  (->> protein
       :features
       (filter (fn [{:keys [type description]}]
                 (and (= "Active site" type)
                      (str/includes? (str/lower-case description)
                                     (str/lower-case active-site-name)))))))

(defn active-site-location
  [active-site]
  (let [start (-> active-site :location :start :value)
        end(-> active-site :location :end :value)]
    (if-not (= start end)
      (throw (ex-info "Weird location annotation! start != end"
                      active-site))
      start)))

(defn go-terms-in-protein
  [protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(and (= "GO" (:database %))))
       (map
        (fn [go-term]
          (let [term (->> (:properties go-term)
                          (filter #(= "GoTerm" (:key %)))
                          first
                          :value)]
            {:id (:id go-term)
             :type (first (#(str/split term #":")))
             :term (second (#(str/split term #":")))})))))

(defn go-terms-in-proteome
  [proteome]
  (->> (map go-terms-in-protein proteome)
       (apply concat)
       #_distinct
       (group-by :type)))

(defn has-go-term?
  [go-term protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(and (= "GO" (:database %))
                     (= go-term (:id %))))
       not-empty))

(defn inactive-entry?
  [entry]
  (= "Inactive" (:entryType entry)))

(defn proteome-lookup
  [proteome]
  (->> proteome
      (map (juxt :primaryAccession identity))
      (into {})))

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

(defn proteome-database-lookup-table
  [db-name proteome & {:keys [just-take-first?]
                       :or {just-take-first? false}}]
  (->> proteome
       (transduce
        (database-lookup-xform db-name :just-take-first? just-take-first?)
        conj
        [])
       (into {})))

(def dois
  (comp
   (partial map
            (comp (partial map :id)
                  (partial filter #(= "DOI" (:database %)))
                  :citationCrossReferences
                  :citation))
   :references))

(def dois+titles+journals
  (comp
   (partial map
            (juxt
             (comp first
                   (partial map :id)
                   (partial filter #(= "DOI" (:database %)))
                   :citationCrossReferences
                   :citation)
             (comp :title :citation)
             (comp :journal :citation)))
   :references))


(def protein-sequence (comp :value :sequence))

(defn active-sites
  [uniprot-record]
  (let [uniprot-record (if (map? uniprot-record)
                         uniprot-record
                         (second uniprot-record))]
    (->> uniprot-record
         :features
         (filter #(#{"Active site"} (:type %))))))

(defn clean-protein
  [protein]
  (-> protein
      (dissoc :organism)
      (dissoc :comments)
      (dissoc :sequence)
      (dissoc :extraAttributes)
      (dissoc :entryAudit)
      (update :uniProtKBCrossReferences
              (partial filter #(#{"AlphaFoldDB"
                                  "KEGG"
                                  "BioCyc"
                                  "UniPathway"
                                  "GO"
                                  "InterPro"} (:database %))))
      (update :features
              (partial filter #(#{"Binding site"
                                  "Active site"
                                  "Transmembrane"
                                  "Topological domain"
                                  "Domain"} (:type %))))
      (assoc :references
             (dois+titles+journals protein))))



(comment
  (require 'user)
  (dois user/uniprot-protein)
  (protein-sequence user/uniprot-protein)
  (active-sites user/uniprot-protein)

  (defn distinct-sequences-count
    [uniprot-proteins]
    (count (distinct (map (comp :value :sequence second) uniprot-proteins))))

  (defn organism-counts
    [uniprot-proteins]
    (let [grouped (group-by :organism uniprot-proteins)]
      (->> grouped
           (map (fn [[k v]]
                  [k (count v)]))
           (into {})
           (sort-by (comp :scientificName first)))))

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

  (defn transpeptidase-sites
    [record]
    (->> (active-sites record)
         (filter #(str/includes? (:description %) "transpeptidase"))))

  (defn has-transpeptidase-site?
    [uniprot-record]
    (-> uniprot-record
        transpeptidase-sites
        not-empty))

  (defn protein-set-description
    [uniprot-proteins]
    (into (sorted-map)
          [{:total-number                                       (->> uniprot-proteins count)
            :has-transpeptidase-annotation                      (->> uniprot-proteins (filter has-transpeptidase-site?) count)
            :number-of-organisms                                (->> uniprot-proteins (group-by :organism) count)
            :number-of-organisms-with-transpeptidase-annotation (->> uniprot-proteins (filter has-transpeptidase-site?) (group-by :organism) count)
            :represented-lineages                               (->> uniprot-proteins (utils/represented-values (comp last :lineage :organism)))
            :protein-evidence                                   (->> uniprot-proteins (count-by :proteinExistence))
            :entry-type                                         (->> uniprot-proteins (count-by :entryType))
            :distinct-sequences                                 (->> uniprot-proteins distinct-sequences-count)
            :distinct-sequences-with-transpeptidase-annotation  (->> uniprot-proteins (filter has-transpeptidase-site?) distinct-sequences-count)}
           (sequence-length-overview uniprot-proteins 20)]))

  (require 'math)

  (defn go-term-map-stats
    [go-term-map]
    (->> (for [[type terms] go-term-map]
           (let [freqs (vals (frequencies terms))]
             [type {:count            (count terms)
                    :mean-frequency   (int (math/mean freqs))
                    :median-frequency (int (math/median freqs))
                    :max-frequency    (apply max freqs)}]))
         (into {})))

  (require '[schmoho.dasudopit.biodb.uniprot.api :as api])
  
  (defonce uniprot-proteome-ecoli
    (api/uniprotkb-stream {:query "taxonomy_id:83333"}))

  (defonce uniprot-proteome-pseudo
    (api/uniprotkb-stream {:query "taxonomy_id:208963"}))

  (let [proteome-1 uniprot-proteome-ecoli
        proteome-2 uniprot-proteome-pseudo
        go-terms-1 (go-terms-in-proteome proteome-1)
        go-terms-2 (go-terms-in-proteome proteome-2)]
    {:proteome-1 (go-term-map-stats go-terms-1)
     :proteome-2 (go-term-map-stats go-terms-2)}))


