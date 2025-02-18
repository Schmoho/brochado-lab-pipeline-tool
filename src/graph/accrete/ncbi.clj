(ns graph.accrete.ncbi
  (:require
   [graph.mapping.ncbi :as mapping]
   [graph.cypher :as cypher]
   [neo4clj.client :as client]))

(def constraints
  ["CREATE CONSTRAINT ncbi_taxon_id FOR (t:NcbiTaxon) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT ncbi_assembly_id FOR (t:NcbiAssembly) REQUIRE t.id IS UNIQUE"])

(defn accrete-taxon-report!
  [connection taxon-report]
  (->> taxon-report
       :reports
       (map mapping/report->neo4j)
       (apply merge-with into)
       #_(client/create-graph! connection)
       (cypher/merge-graph! connection)))
