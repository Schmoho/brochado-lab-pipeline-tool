(ns user
  (:require
   [biodb.ncbi.api :as ncbi.api]
   [biodb.uniprot.api :as uniprot.api]
   [clojure.java.io :as io]
   [portal.api :as p]
   [portal.viewer :as v]
   [utils :as utils]))

;; REPL data setup
(do
  (def brochado-strains
    (utils/read-file  (io/resource "brochado-strains.edn")))
  (def ncbi-tax-id "652611")
  (def ncbi-taxon-report
    (ncbi.api/taxon-dataset-report ncbi-tax-id {"page_size" 1000}))
  (def ncbi-genome-annotation-summary
    (ncbi.api/genome-annotation-summary "GCA_000006765.1"))

  (def ncbi-genome-annotation-report
    (ncbi.api/genome-annotation-report "GCA_000006765.1"))

  (def ncbi-genome-dataset-report
    (ncbi.api/genome-dataset-report "GCA_000006765.1"))

  (def uniprot-taxonomy-pao1
    (uniprot.api/taxonomy-entry "208964"))

  (def uniprot-taxonomy-ecoli
    (uniprot.api/taxonomy-entry "83333"))

  (def uniprot-proteome
    (first (uniprot.api/proteomes-by-taxon-id "208964")))

  (def uniprot-protein
    (uniprot.api/uniprotkb-entry "G3XCV0"))

  (def uniparc-protein
    (uniprot.api/uniparc-entry "UPI00053A1130"))

  #_(def uniprot-proteome-proteins
      (uniprot.api/proteins-by-proteome "UP000002438"))

  (def uniprot-protein-cross-references
    (:uniProtKBCrossReferences uniprot-protein)))

(do
  (def defaults
    {string? v/text
     bytes?  v/bin})

  (defn- get-viewer-f [value]
    (or (some (fn [[predicate viewer]]
                (when (predicate value)
                  viewer))
              defaults)
        v/tree))

  (comment
    (defn default-submit [value]
      (let [f (get-viewer-f value)]
        (p/submit (f value))))

    (add-tap #'default-submit))

  ;; this just pipes every eval in the REPL to portal
  ;; - it's amazing
  (defn submit [value]
    (if (-> value meta :portal.nrepl/eval)
      (let [{:keys [stdio report result]} value]
        (when stdio (p/submit stdio))
        (when report (p/submit report))
        (p/submit result))
      (p/submit value)))

  (add-tap submit)
  (comment (remove-tap submit))

  (def p (p/open  {:portal.colors/theme :portal.colors/solarized-light}))
  (comment (p/close p)))

(comment
  (require '[clojure.spec.alpha.test :as st])
  (st/instrument)
  ;; (alter-var-root #'s/*explain-out* (constantly expound/printer))

   ;; this documents how data needs to be formatted to be printeable
  ;; in fancy ways by portal
  (require '[clojure.spec.alpha :as sp])

;; collection of maps of [{:x 0 :y 0} ...] maps
  (sp/def :tabular/x number?)
  (sp/def :tabular/y number?)
  (sp/def ::data
    (sp/keys :req-un [:tabular/x :tabular/y]))
  (sp/def ::tabular-data
    (sp/coll-of ::data :min-count 2))

;; :x [0 1 2 ...] :y [0 1 2 ...]
  (sp/def :numerical-coll/x
    (sp/coll-of number? :min-count 2))
  (sp/def :numerical-coll/y
    (sp/coll-of number? :min-count 2))
  (sp/def ::numerical-collection
    (sp/keys :req-un [:numerical-coll/x :numerical-coll/y]))

  (s/exercise ::tabular-data)

;; dates back to when I was playing around with Neo4j
  (require '[neo4clj.client :as client])
  (defn clear-db!
    [connection]
    (client/execute! connection "MATCH (n) DETACH DELETE n")))
