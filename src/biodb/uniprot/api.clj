(ns biodb.uniprot.api
  (:require
   [biodb.http :as http]
   [cheshire.core :as json]
   [clojure.tools.logging :as log]))

(def uniprot-api-base "https://rest.uniprot.org")

;; --- Simple Endpoint Wrappers ---

;; ;; Taxonomy

(def taxon-meta
  {:biodb/source :uniprot
   :uniprot/type :taxon})

(defn taxonomy-entry
  ([taxon-id]
   (taxonomy-entry taxon-id {}))
  ([taxon-id query-params]
   (let [url   (format "%s/taxonomy/%s" uniprot-api-base taxon-id)
         _     (log/debug "Query" url "with" query-params)
         taxon (-> (http/get url {:query-params query-params})
                   (:body)
                   (json/parse-string true))]
     (with-meta taxon taxon-meta))))

#_(:lineage (taxonomy-entry "208964"))

;; ;; Proteomes
(def proteome-meta
  {:biodb/source :uniprot
   :uniprot/type :proteome})

(defn proteomes-entry
  ([proteome-id]
   (proteomes-entry proteome-id {}))
  ([proteome-id query-params]
   (let [url      (format "%s/proteomes/%s" uniprot-api-base proteome-id)
         _        (log/debug "Query" url "with" query-params)
         proteome (-> (http/get url {:query-params query-params})
                      (:body)
                      (json/parse-string true))]
     (with-meta proteome proteome-meta))))

#_(proteomes-entry "UP000014183")

(defn proteomes-search
  [query-params]
  (let [url       (format "%s/proteomes/search" uniprot-api-base)
        _         (log/debug "Query" url "with" query-params)
        proteomes (-> (http/get url {:query-params query-params})
                      (:body)
                      (json/parse-string true)
                      :results)]
    (mapv #(with-meta % proteome-meta) proteomes)))

#_(proteomes-search {:query "208964"})

;; ;; Proteins

(def uniprotkb-entry-meta
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry})

(defn uniprotkb-entry
  ([accession]
   (uniprotkb-entry accession {}))
  ([accession query-params]
   (let [url   (format "%s/uniprotkb/%s" uniprot-api-base accession)
         _     (log/debug "Query" url "with" query-params)
         entry (-> (http/get url {:query-params query-params})
                   (:body)
                   (json/parse-string true))]
     (with-meta entry uniprotkb-entry-meta))))

#_(uniprotkb-entry "G3XCV0")

(defn uniprotkb-search
  [query-params]
  (let [url               (format "%s/uniprotkb/search" uniprot-api-base)
        _                 (log/debug "Query" url "with" query-params)
        uniprotkb-entries (-> (http/get url {:query-params query-params})
                              (:body)
                              (json/parse-string true)
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
                             (json/parse-string true)
                             :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) uniprotkb-entries)))

#_(uniprotkb-stream {:query "proteome:UP000002438"})

;; Uniparc

(def uniparc-entry-meta
  {:biodb/source             :uniprot
   :uniprot/type             :uniparc-entry
   :biotools/fasta-mappable? true})

(defn uniparc-entry
  ([accession]
   (uniparc-entry accession {}))
  ([accession query-params]
   (let [url   (format "%s/uniparc/%s" uniprot-api-base accession)
         _     (log/debug "Query" url "with" query-params)
         entry (-> (http/get url {:query-params query-params})
                   (:body)
                   (json/parse-string true))]
     (with-meta entry uniparc-entry-meta))))

#_(uniparc-entry "UPI00053A1130")

;; ;; Uniref

(def uniref-meta
  {:biodb/source :uniprot
   :uniprot/type :uniref-entry})

(defn uniref-entry
  ([uniref-id]
   (uniref-entry uniref-id {}))
  ([uniref-id query-params]
   (let [url   (format "%s/uniref/%s"
                     uniprot-api-base
                     uniref-id)
         _     (log/debug "Query" url "with" query-params)
         entry (-> (http/get url {:query-params query-params})
                   (:body)
                   (json/parse-string true))]
     (with-meta entry uniref-meta))))

#_(uniref-entry #_"UniRef50_Q4K603"
                "UniRef90_G3XD31"
                #_"UniRef100_A0A0U4NUB5")

(defn uniref-search
  [query-params]
  (let [url     (format "%s/uniref/search" uniprot-api-base)
        _       (log/debug "Query" url "with" query-params)
        entries (-> (http/get url {:query-params query-params})
                    (:body)
                    (json/parse-string true)
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
                         (json/parse-string true)
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
        taxons(-> http-result
                        :body
                        (json/parse-string true)
                        :results)]
    taxons))

(defn proteins-by-proteome
  [proteome-id]
  (let [url         (format "%s/uniprotkb/stream" uniprot-api-base)
        _           (log/debug "Query" url "for" proteome-id)
        http-result (clj-http.client/get
                     url
                     {:query-params
                      {:query (format "proteome:%s" proteome-id)}})
        proteins    (-> http-result
                        :body
                        (json/parse-string true)
                        :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) proteins)))

#_(for [proteome '("UP000002438" "UP001223114" "UP001235792" "UP001241625")]
    (count (proteins-by-proteome proteome)))

#_(->> (proteins-by-proteome "UP000002438")
       (map :primaryAccession))

(defn proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query taxon-id})
       :results
       (filter #(= (if-not (number? taxon-id)
                     (parse-long taxon-id) taxon-id)
                   (-> % :taxonomy :taxonId)))
       (mapv #(with-meta % proteome-meta))))

#_(->> (proteomes-by-taxon-id "208964")
       (map :genomeAssembly))

(defn uniref-by-protein-id
  [protein-id]
  (uniref-search {:query (format "uniprot_id:%s" protein-id)}))

#_(map :id (:results (uniref-by-protein-id "A0A0H2ZHP9")))




;;;; Stuff

;; (defn get-inactive-record-via-parc!
;;   [{:keys [extraAttributes]}]
;;   (let [uniparc-record
;;         (get-json
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
;;        (#(get-json
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
