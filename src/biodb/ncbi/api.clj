(ns biodb.ncbi.api
  (:require
   [biodb.http :as http]
   [clojure.string :as str]
   [cheshire.core :as json]))

(def ncbi-api-base "https://api.ncbi.nlm.nih.gov/datasets/v2")

(defn taxon-dataset-report
  ([taxon-ids]
   (taxon-dataset-report taxon-ids {"page_size" 1000}))
  ([taxon-ids query-params]
   (let [url (format "%s/genome/taxon/%s/dataset_report"
                     ncbi-api-base
                     (if (coll? taxon-ids)
                       (str/join "," taxon-ids)
                       taxon-ids))]
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(def taxon-report (taxon-dataset-report "652611" {"page_size" 1000}))
#_(-> taxon-report keys)
;; => (:reports :total_count)
#_(-> taxon-report :reports first keys)
;; => (:accession :annotation_info :assembly_info :assembly_stats :average_nucleotide_identity :checkm_info :current_accession :organism :paired_accession :source_database)
#_(-> taxon-report :reports first :organism keys)
;; => (:tax_id :organism_name :infraspecific_names)

(defn genome-annotation-summary
  ([accessions]
   (genome-annotation-summary accessions {}))
  ([accessions query-params]
   (let [url (format "%s/genome/accession/%s/annotation_summary"
                     ncbi-api-base
                     (if (coll? accessions)
                       (str/join "," accessions)
                       accessions))]
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(genome-annotation-summary "GCA_000006765.1")

(defn genome-annotation-report
  ([accessions]
   (genome-annotation-report accessions {"page_size" 1000}))
  ([accessions query-params]
   (let [url (format "%s/genome/accession/%s/annotation_report"
                     ncbi-api-base
                     (if (coll? accessions)
                       (str/join "," accessions)
                       accessions))]
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(genome-annotation-report "GCA_000006765.1")


(defn genome-dataset-report
  ([accessions]
   (genome-dataset-report accessions {"page_size" 1000}))
  ([accessions query-params]
   (let [url (format "%s/genome/accession/%s/dataset_report"
                     ncbi-api-base
                     (if (coll? accessions)
                       (str/join "," accessions)
                       accessions))]
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(genome-dataset-report "GCA_000006765.1")
