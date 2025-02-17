(ns graph.load.uniprot
  (:require
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.mapping.uniprot.core :as mapping]))

(def constraints
  ["CREATE CONSTRAINT uniprot_database_id FOR (t:Database) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT uniprot_taxon_id FOR (t:UniprotTaxon) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT uniprot_proteome_id FOR (t:UniprotProteome) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT uniprot_protein_id FOR (t:UniprotProtein) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT uniprot_protein_feature_id FOR (t:UniprotProteinFeature) REQUIRE t.id IS UNIQUE"
   "CREATE CONSTRAINT uniprot_cross_referenced_entity_id FOR (t:UniprotCrossReferencedEntity) REQUIRE t.id IS UNIQUE"])

(defn load-database!
  [connection database]
  (log/debug "Loading Uniprot DB into DB.")
  (->> database
       (mapping/database->database-node)
       #_(client/create-node! connection)
       (cypher/merge-node! connection)))

(defn load-taxon!
  [connection taxon]
  (log/debug "Loading Uniprot Taxon into DB.")
  (->> taxon
       (mapping/taxon->neo4j)
       #_(client/create-graph! connection)
       #_(cypher/merge-graph! connection)
       (cypher/merge-node-with-rels-by-id! connection)))

(defn load-proteome!
  [connection proteome]
  (->> proteome
       (log/debug "Loading Uniprot Proteome into DB.")
       (mapping/proteome->neo4j)
       #_(client/create-graph! connection)
       (cypher/merge-graph! connection)))

(defn load-protein!
  [connection protein]
  (log/debug "Loading Uniprot Protein into DB.")
  (->> protein
       (mapping/protein->neo4j)
       #_(client/create-graph! connection)
       #_(cypher/merge-graph! connection)
       (cypher/merge-node-with-rels-by-id! connection)))

#_(def insane-taxonomic-levels
  #{"no rank"
    "phylum"
    "class"
    "order"
    "family"
    "superkingdom"
    "kingdom"
    "clade"
    "subkingdom"
    "subphylum"
    "subclass"
    "suborder"
    "subfamily"
    "tribe"})
