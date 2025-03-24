(ns schmoho.biodb.uniprot.api
  (:require
   [schmoho.utils.walk :as utils]
   [schmoho.biodb.http :as http]
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clj-http.client :as clj-http.client]))

(def uniprot-api-base "https://rest.uniprot.org")

;; --- Simple Endpoint Wrappers ---

;; ;; Taxonomy

(def taxon-meta
  {:biodb/source :uniprot
   :uniprot/type :taxon})

(def taxon-fields-of-interest
  ["common_name" "id" "lineage" "rank" "scientific_name" "statistics"])

(def taxonomy-entry
  (http/id-query
   (str (format "%s/taxonomy/" uniprot-api-base) "%s")
   taxon-meta))

#_(:lineage (taxonomy-entry "208964" {:fields taxon-fields-of-interest}))

;; ;; Proteomes

(def proteome-meta
  {:biodb/source :uniprot
   :uniprot/type :proteome})

(def proteome-fields-of-interest
  ["organism_id" "protein_count" "upid"])

(def proteomes-entry
  (http/id-query
   (str (format "%s/proteomes/" uniprot-api-base) "%s")
   proteome-meta))

#_(proteomes-entry "UP000014183")

(defn proteomes-search
  [query-params]
  (let [url       (format "%s/proteomes/search" uniprot-api-base)
        query-params (update query-params :fields #(if (coll? %)
                                                     (str/join "," %)
                                                     %))
        _         (log/debug "Query" url "with" query-params)
        proteomes (-> (http/get url {:query-params query-params})
                      (:body)
                      (json/parse-string)
                      (utils/white-space-safe-keywordize-keys)
                      :results)]
    (mapv #(with-meta % proteome-meta) proteomes)))

;; ;; Proteins

(def uniprotkb-entry-meta
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry})

(def protein-fields-of-interest
  ["accession"
   "cc_domain"
   "cc_pathway"
   "ec"
   "ft_act_site"
   "ft_binding"
   "ft_dna_bind"
   "ft_domain"
   "ft_region"
   "ft_site"
   "ft_topo_dom"
   "ft_transmem"
   "ft_zn_fing"
   "gene_names"
   "go"
   "protein_existence"
   "protein_families"
   "protein_name"
   "sequence"
   "structure_3d"
   "xref_alphafolddb"
   "xref_biocyc"
   "xref_drugbank"
   "xref_drugcentral"
   "xref_interpro"
   "xref_kegg"
   "xref_pdb"
   "xref_unipathway"])

(def uniprotkb-entry
  (http/id-query
   (str (format "%s/uniprotkb/" uniprot-api-base) "%s")
   uniprotkb-entry-meta))

#_(uniprotkb-entry "G3XCV0" #_{:fields protein-fields-of-interest})

(defn uniprotkb-search
  [query-params]
  (let [url               (format "%s/uniprotkb/search" uniprot-api-base)
        _                 (log/debug "Query" url "with" query-params)
        uniprotkb-entries (-> (http/get url {:query-params query-params})
                              (:body)
                              (json/parse-string)
                              (utils/white-space-safe-keywordize-keys)
                              :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) uniprotkb-entries)))

#_(uniprotkb-search {:query "proteome:UP000002438"})

#_(uniprotkb-search {:query (format "gene:%s AND taxonomy_name:%s"
                                    "mrcb" "Pseudomonas aeruginosa")})

(defn uniprotkb-stream
  [query-params]
  (let [url               (format "%s/uniprotkb/stream" uniprot-api-base)
        _                 (log/debug "Query" url "with" query-params)
        uniprotkb-entries (-> (clj-http.client/get url {:query-params query-params})
                              (:body)
                              (json/parse-string)
                              (utils/white-space-safe-keywordize-keys)
                              :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) uniprotkb-entries)))

#_(uniprotkb-stream {:query "proteome:UP000002438"})

;; Uniparc

(def uniparc-entry-meta
  {:biodb/source             :uniprot
   :uniprot/type             :uniparc-entry})

(def uniparc-entry
  (http/id-query
   (str (format "%s/uniparc/" uniprot-api-base) "%s")
   uniparc-entry-meta))

#_(uniparc-entry "UPI00053A1130")

;; ;; Uniref

(def uniref-meta
  {:biodb/source :uniprot
   :uniprot/type :uniref-entry})

(def uniref-entry
  (http/id-query
   (str (format "%s/uniref/" uniprot-api-base) "%s")
   uniref-meta))

#_(uniref-entry #_"UniRef50_Q4K603"
                "UniRef90_G3XD31"
                #_"UniRef100_A0A0U4NUB5")

(defn uniref-search
  [query-params]
  (let [url     (format "%s/uniref/search" uniprot-api-base)
        _       (log/debug "Query" url "with" query-params)
        entries (-> (http/get url {:query-params query-params})
                    (:body)
                    (json/parse-string)
                    (utils/white-space-safe-keywordize-keys)
                    :results)]
    (mapv #(with-meta % uniref-meta) entries)))

#_(uniref-search {:query "uniprot_id:A0A0H2ZHP9"})

;; ;; Databases

(def database-meta
  {:biodb/source :uniprot
   :uniprot/type :database-reference})

(def databases
  (memoize
   (fn databases
     []
     (let [url         "https://rest.uniprot.org/database/stream?format=json&query=%28*%29"
           _           (log/debug "Query" url)
           http-result (clj-http.client/get url)
           databases   (-> http-result
                           :body
                           (json/parse-string)
                           (utils/white-space-safe-keywordize-keys)
                           :results)]
       (mapv #(with-meta % database-meta) databases)))))


;; --- Convenience Wrappers ---

(defn downstream-lineage
  [taxon-id]
  (let [url         (format "%s/taxonomy/stream" uniprot-api-base)
        _           (log/debug "Query" url "for" taxon-id)
        http-result (clj-http.client/get
                     url
                     {:query-params
                      {:query (format "ancestor:%s" taxon-id)}})
        taxons      (-> http-result
                        :body
                        (json/parse-string)
                        (utils/white-space-safe-keywordize-keys)
                        :results)]
    taxons))

#_(count (downstream-lineage 136841))

(defn proteins-by-proteome
  [proteome-id]
  (let [url         (format "%s/uniprotkb/stream" uniprot-api-base)
        _           (log/debug "Query" url "for" proteome-id)
        http-result (clj-http.client/get
                     url
                     {:query-params
                      {:query (format "proteome:%s" proteome-id)
                       :fields protein-fields-of-interest}})
        proteins    (-> http-result
                        :body
                        (json/parse-string)
                        (utils/white-space-safe-keywordize-keys)
                        :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) proteins)))

#_(for [proteome '("UP000002438" "UP001223114" "UP001235792" "UP001241625")]
    (count (proteins-by-proteome proteome)))

#_(time (count (proteins-by-proteome "UP000002438")))

(defn ref-proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query (format "organism_id=%s AND proteome_type=\"1\"" taxon-id)
                          :fields proteome-fields-of-interest})
       (mapv #(with-meta % proteome-meta))))

#_(ref-proteomes-by-taxon-id "208964")

(defn proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query (format "organism_id=%s" taxon-id)
                          :fields proteome-fields-of-interest})
       (mapv #(with-meta % proteome-meta))))

#_(proteomes-by-taxon-id "83333")

(defn uniref-by-protein-id
  [protein-id]
  (uniref-search {:query (format "uniprot_id:%s" protein-id)}))

#_(map :id (:results (uniref-by-protein-id "A0A0H2ZHP9")))

(defn uniref-entries-by-protein-id
  ([protein-id] (uniref-entries-by-protein-id protein-id #{"UniRef90" "UniRef100"}))
  ([protein-id uniref-cluster-types]
   (let [uniref-clusters (uniref-by-protein-id protein-id)]
     (->> uniref-clusters
          (filter #((set uniref-cluster-types) (:entryType %)))
          (map (comp (fn [[type entry-id]]
                        [type (-> entry-id uniref-entry :members)])
                      (juxt :entryType :id)))
          (into {})))))

#_(uniref-entries-by-protein-id "A0A0H2ZHP9")

(defn uniref-proteins-by-protein-id
  ([protein-id] (uniref-proteins-by-protein-id protein-id
                                               #{"UniRef90" "UniRef100"}))
  ([protein-id uniref-cluster-types]
   (let [cluster (uniref-entries-by-protein-id protein-id
                                               uniref-cluster-types)]
     (->> (for [[cluster-type cluster] cluster]
            (let [{:keys [uni-prot-kb-id uni-parc]}
                  (-> (group-by :memberIdType cluster)
                      (update-keys csk/->kebab-case-keyword))]
              [cluster-type
               {:uniprotkb (->> uni-prot-kb-id
                                (mapcat :accessions)
                                (distinct)
                                (mapv uniprotkb-entry))
                :uniparc   (->> uni-parc
                                (map :memberId)
                                (distinct)
                                (mapv uniparc-entry))}]))
          (into {})))))

#_(uniref-proteins-by-protein-id "A0A0H2ZHP9")

(defn proteins-by-taxa-and-genes
  [taxa genes]
  (let [or-query-part (fn [search-term input]
                        (str "("
                             (str/join " OR "
                                       (map (partial format (str search-term ":%s")) input))
                             ")"))
        gene-query-part (or-query-part "gene" genes)
        taxa-query-part (or-query-part "taxonomy_name" taxa)]
    (uniprotkb-search
     {:query (str gene-query-part " AND " taxa-query-part)})))

#_(count (map :organism (proteins-by-taxa-and-genes ["'Pseudomonas aeruginosa'"] ["mrcA" "mrcB"])))

;;;; Stuff

;; (defn get-inactive-record-via-parc!
;;   [{:keys [extraAttributes]}]
;;   (let [uniparc-record
;;         (http/get
;;          (str "https://rest.uniprot.org/uniparc/"
;;               (:uniParcId extraAttributes)))]
;;     uniparc-record))

;; (defn get-kb-entry-for-parc-entry!
;;   [parc]
;;   (->> parc
;;        :uniParcCrossReferences
;;        (sort-by :lastUpdated)
;;        reverse
;;        (filter #(str/includes? (:database %) "UniProtKB"))
;;        first
;;        :id
;;        (#(http/get
;;           (str "https://www.uniprot.org/uniprotkb/" % ".json")))))

;; (defn get-uniparc-results-for-invalid-uniprot-entries!
;;   [uniprot-records]
;;   (->> uniprot-records
;;        (filter (comp :inactiveReason second))
;;        (map (juxt first
;;                   (comp get-kb-entry-for-parc-entry!
;;                         get-inactive-record-via-parc!
;;                         second)))
;;        (into {})))
