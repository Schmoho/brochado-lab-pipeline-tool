(ns schmoho.biodb.uniprot.core
  (:require
   [clojure.string :as str]
   [schmoho.biodb.uniprot.formats]
   [clojure.set :as set]))

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

(defn feature->location
  [protein-feature]
  [(-> protein-feature :location :start :value)
   (-> protein-feature :location :end :value)])

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

(defn protein-go-terms
  [protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(= "GO" (:database %)))
       (map (fn [go-term]
              {:id (:id go-term)
               :label (->> (:properties go-term)
                           (filter #(= "GoTerm" (:key %)))
                           first
                           :value)}))))

(defn proteome-go-terms
  [proteome]
  (->> proteome
      (mapcat protein-go-terms)
      set))

(defn go-term-filtering-fn
  [proteome go-term]
  (let [proteome-lookup
        (->> proteome
             (map (juxt :primaryAccession identity))
             (into {}))]
    (fn [table-row]
      (some->> table-row
               :protein_id
               proteome-lookup
               (has-go-term? go-term)))))

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

(defn has-afdb?
  [protein]
  (->> (:uniProtKBCrossReferences protein)
       (filter #(= "AlphaFoldDB" (:database %)))
       seq))
