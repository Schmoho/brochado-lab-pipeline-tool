(ns biodb.uniprot.api
  (:require
   [biodb.http :as http]
   [cheshire.core :as json]
   [clojure.tools.logging :as log]))

(def uniprot-api-base "https://rest.uniprot.org/")

(defn taxonomy-entry
  ([taxon-id]
   (taxonomy-entry taxon-id {}))
  ([taxon-id query-params]
   (let [url (format "%s/taxonomy/%s"
                     uniprot-api-base
                     taxon-id)]
     (log/debug "Query" url "with" query-params)
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(:lineage (taxonomy-entry "208964"))

(defn proteomes-search
  [query-params]
  (let [url (format "%s/proteomes/search"
                    uniprot-api-base)]
    (log/debug "Query" url "with" query-params)
    (-> (http/get url {:query-params query-params})
        (:body)
        (json/parse-string true))))

#_(proteomes-search {:query "208964"})

(defn proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query taxon-id})
       :results
       (filter #(= (if-not (number? taxon-id)
                     (parse-long taxon-id) taxon-id)
                   (-> % :taxonomy :taxonId)))))

#_(->> (proteomes-by-taxon-id "208964")
       (map :genomeAssembly))

(defn uniprotkb-entry
  ([accession]
   (uniprotkb-entry accession {}))
  ([accession query-params]
   (let [url (format "%s/uniprotkb/%s"
                     uniprot-api-base
                     accession)]
     (log/debug "Query" url "with" query-params)
     (-> (http/get url {:query-params query-params})
         (:body)
         (json/parse-string true)))))

#_(uniprotkb-entry "G3XCV0")

(defn uniprotkb-search
  [query-params]
  (let [url (format "%s/uniprotkb/search"
                    uniprot-api-base)]
    (log/debug "Query" url "with" query-params)
    (-> (http/get url {:query-params query-params})
        (:body)
        (json/parse-string true))))

#_(uniprotkb-search {:query "proteome:UP000002438"})

(defn uniprotkb-stream
  [query-params]
  (let [url (format "%s/uniprotkb/stream"
                    uniprot-api-base)]
    (log/debug "Query" url "with" query-params)
    (-> (clj-http.client/get url {:query-params query-params})
        (:body)
        (json/parse-string true))))

#_(uniprotkb-stream {:query "proteome:UP000002438"})

(defn proteins-by-proteome
  [proteome-id]
  (let [url (format "%s/uniprotkb/stream"
                    uniprot-api-base)]
    (let [_           (log/debug "Query" url "for" proteome-id)
          http-result (clj-http.client/get
                       url
                       {:query-params
                        {:query (format "proteome:%s"
                                        proteome-id)}})
          proteins    (-> http-result
                          :body
                          (json/parse-string true)
                          :results)]
      proteins)))

#_(for [proteome '("UP000002438" "UP001223114" "UP001235792" "UP001241625")]
    (count (proteins-by-proteome proteome)))

#_(->> (proteins-by-proteome "UP000002438")
       (map :primaryAccession))

(def databases
  (memoize
   (fn databases
     []
     (let [url         "https://rest.uniprot.org/database/stream?format=json&query=%28*%29"
           _           (log/debug "Query" url)
           http-result (clj-http.client/get
                        url)]
       (-> http-result
           :body
           (json/parse-string true)
           :results)))))


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
