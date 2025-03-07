(ns biodb.uniprot.taxonomy
  (:require [biodb.uniprot.api :as api]))

;; "no rank"
;; "clade"

(def taxonomic-level-order
  (zipmap
   ["superkingdom" ;; domain
    "kingdom"
    "subkingdom"
    "phylum"
    "subphylum"
    "class"
    "subclass"
    "order"
    "suborder"
    "family"
    "subfamily"
    "tribe"
    "genus"
    "species"
    "strain"]
   (range)))

(defn get-taxonomy-filter
  [filter-taxon]
  (some->> (api/downstream-lineage filter-taxon)
           (map :taxonId)
           set))
