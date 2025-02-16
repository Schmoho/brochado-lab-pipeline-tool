(ns user
  (:require
   [clj-http.client :as http]
   [neo4clj.client :as client]
   [biodb.ncbi.api :as ncbi.api]
   [biodb.uniprot.api :as uniprot.api]
   [cheshire.core :as json]
   [biodb.uniprot.core :as uniprot]))

(def last-val (atom nil))
(def last-val-tap #(reset! last-val %))
(add-tap last-val-tap)

#_(add-tap (bound-fn* clojure.pprint/pprint))

#_[expound.alpha :as expound]
#_(alter-var-root #'s/*explain-out* (constantly expound/printer))
#_[clojure.spec.test.alpha :as st]
#_(st/instrument)

(def ncbi-tax-id "652611")

(def ncbi-taxon-report
  (ncbi.api/taxon-dataset-report ncbi-tax-id {"page_size" 1000}))

(def ncbi-genome-annotation-summary
  (ncbi.api/genome-annotation-summary "GCA_000006765.1"))

(def ncbi-genome-annotation-report
  (ncbi.api/genome-annotation-report "GCA_000006765.1"))

(def ncbi-genome-dataset-report
  (ncbi.api/genome-dataset-report "GCA_000006765.1"))

(def uniprot-taxonomy-entry
  (uniprot.api/taxonomy-entry "208964"))

(def uniprot-proteome
  (first (uniprot.api/proteomes-by-taxon-id "208964")))

(def uniprot-protein
  (uniprot.api/uniprotkb-entry "G3XCV0"))

#_(def uniprot-proteome-proteins
    (uniprot.api/proteins-by-proteome "UP000002438"))

(defn clear-db!
  [connection]
  (client/execute! connection "MATCH (n) DETACH DELETE n"))

(defn first-map-level
  [m]
  (reduce-kv
   (fn [acc k v]
     (if (not (coll? v))
       (assoc acc k v)
       acc))
   {}
   m))

(defn get
  [url]
  (-> (http/get url)
      :body
      (json/parse-string true)))


(def uniprot-protein-cross-references
  (:uniProtKBCrossReferences uniprot-protein))


(defn ->uniprot-protein-cross-references
  [db]
  (filter #(= db (:database %))
          (:uniProtKBCrossReferences uniprot-protein)))

