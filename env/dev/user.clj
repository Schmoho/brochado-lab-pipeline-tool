(ns user
  (:require
   [portal.api :as p]
   [portal.viewer :as v]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [expound.alpha :as expound]
   [clj-http.client :as http]
   [neo4clj.client :as client]
   [biodb.ncbi.api :as ncbi.api]
   [biodb.uniprot.api :as uniprot.api]
   [cheshire.core :as json]
   [biodb.uniprot.core :as uniprot]))

;; (def last-val (atom nil))
;; (def last-val-tap #(reset! last-val %))
;; (add-tap last-val-tap)
;; @last-val
;; #_(add-tap (bound-fn* clojure.pprint/pprint))

;; (alter-var-root #'s/*explain-out* (constantly expound/printer))

;; (st/instrument)

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

(def uniparc-protein
  (uniprot.api/uniparc-entry "UPI00053A1130"))

#_(def uniprot-proteome-proteins
    (uniprot.api/proteins-by-proteome "UP000002438"))

(def uniprot-protein-cross-references
  (:uniProtKBCrossReferences uniprot-protein))

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

(defn ->uniprot-protein-cross-references
  [db]
  (filter #(= db (:database %))
          (:uniProtKBCrossReferences uniprot-protein)))

(def p (p/open  {:portal.colors/theme :portal.colors/solarized-light}))

#_(p/close p )



(def defaults
  {string? v/text
   bytes?  v/bin})

(defn- get-viewer-f [value]
  (or (some (fn [[predicate viewer]]
              (when (predicate value)
                viewer))
            defaults)
      v/tree))

(defn default-submit [value]
  (let [f (get-viewer-f value)]
    (p/submit (f value))))

;; (add-tap #'default-submit)

;; If you would like to process eval results, you can do so with a custom tap handler, such as the following:

(defn submit [value]
  (if (-> value meta :portal.nrepl/eval)
    (let [{:keys [stdio report result]} value]
      (when stdio (p/submit stdio))
      (when report (p/submit report))
      (p/submit result))
    (p/submit value)))

(add-tap submit)


;; (remove-tap submit)

;; (tap> ncbi-tax-id)

;; (tap> ncbi-taxon-report)

;; (tap> ncbi-genome-annotation-summary)

;; (tap> ncbi-genome-annotation-report)

;; (tap> ncbi-genome-dataset-report)

;; (tap> uniprot-taxonomy-entry)

;; (tap> uniprot-proteome)

;; (tap> (with-meta uniprot-protein
;;         {:name (format "Uniprot Protein %s" (:primaryAccession uniprot-protein))}))

;; (tap> (frequencies uniprot-protein-cross-references))

;; (require '[clojure.spec.alpha :as sp])

;; ;; collection of maps of [{:x 0 :y 0} ...] maps
;; (sp/def :tabular/x number?)
;; (sp/def :tabular/y number?)
;; (sp/def ::data
;;   (sp/keys :req-un [:tabular/x :tabular/y]))
;; (sp/def ::tabular-data
;;   (sp/coll-of ::data :min-count 2))

;; ;; :x [0 1 2 ...] :y [0 1 2 ...]
;; (sp/def :numerical-coll/x
;;   (sp/coll-of number? :min-count 2))
;; (sp/def :numerical-coll/y
;;   (sp/coll-of number? :min-count 2))
;; (sp/def ::numerical-collection
;;   (sp/keys :req-un [:numerical-coll/x :numerical-coll/y]))

;; (s/exercise ::tabular-data)

