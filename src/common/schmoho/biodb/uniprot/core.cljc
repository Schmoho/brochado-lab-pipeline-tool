(ns schmoho.biodb.uniprot.core
  (:require
   [clojure.string :as str]
   [schmoho.formats.fasta :refer [->fasta]]))

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
