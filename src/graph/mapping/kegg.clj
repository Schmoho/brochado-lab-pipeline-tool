(ns graph.mapping.kegg
  (:require
   [clojure.spec.alpha :as s]
   [graph.mapping.utils :refer :all]
   [graph.mapping.uniprot.ncbi :as ncbi]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.tools.logging :as log]
   [neo4clj.query-builder :as builder]
   [clojure.spec.test.alpha :as st]))

(defn cds-ref-id [cds] (sanitize-ref-id (str "G_KEGG_" (:id cds))))

(defn cds->cds-node
  [cds]
  (let [ref-id (cds-ref-id cds)]
    {:nodes   [{:ref-id ref-id
                :labels [:cds :kegg-cds]
                :props  {:symbols    (:symbol cds)
                         :name       (:name cds)
                         :entry-type (:entry-type cds)
                         :id         (:id cds)
                         :t-number   (:t-number cds)}}]
     :rels    [{:ref-id "asdf"
                :type :contains
                :from {:ref-id "D"
                       :labels [:database]
                       :props  {:id "KEGG"}}
                :to   {:ref-id ref-id
                       :props  {:id (:id cds)}}}]}))

(s/fdef cds->cds-node
  :args (s/cat :cds map?)
  :ret :cypher/graph)
