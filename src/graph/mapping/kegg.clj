(ns graph.mapping.kegg
  (:require [graph.mapping.utils :refer :all]
            [graph.mapping.uniprot.ncbi :as ncbi]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

(defn cds-ref-id [cds] (sanitize-ref-id (str "G_KEGG_" (:id cds))))

(defn cds->cds-node
  [cds]
  (log/debug "Mapping CDS to DB node.")
  {:ref-id   (cds-ref-id cds)
   :labels   [:cds :kegg-cds]
   :props    {:symbols    (:symbol cds)
              :name       (:name cds)
              :entry-type (:entry-type cds)
              :id         (:id cds)
              :t-number   (:t-number cds)}})
