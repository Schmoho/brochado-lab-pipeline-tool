(ns graph.accrete.uniprot
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

(defn accrete-database!
  [connection database]
  (log/debug "Accreteing Uniprot DB into DB.")
  (->> database
       (mapping/database->database-node)
       (cypher/merge-node! connection)))

(defn accrete-taxon!
  [connection taxon]
  (log/debug "Accreteing Uniprot Taxon into DB.")
  (let [result (->> taxon
                    (mapping/taxon->neo4j)
                    (cypher/merge-node-with-rels-by-id! connection))]
    {:input-data taxon
     :db-result  result
     :type       :uniprot/taxon
     :id         (:taxonId taxon)}))

(defn accrete-proteome!
  [connection proteome]
  (log/debug "Accreteing Uniprot Proteome into DB.")
  (let [result (->> proteome
                    (mapping/proteome->neo4j)
                    (cypher/merge-graph! connection))]
    {:input-data proteome
     :db-result  result
     :type       :uniprot/proteome
     :id         (:id proteome)}))

(defn accrete-protein!
  [connection protein]
  (log/debug "Accreteing Uniprot Protein into DB.")
  (let [result (->> protein
                    (mapping/protein->neo4j)
                    (cypher/merge-node-with-rels-by-id! connection))]
    {:input-data protein
     :db-result  result
     :type       :uniprot/protein
     :id         (:primaryAccession protein)}))

(defn connect-protein!
  [connection protein]
  (let [protein-id (:primaryAccession protein)
        cross-ids  (mapping/protein->cross-ids protein)]
    (doseq [[db ids] cross-ids
            id       ids]
      (log/info "Connecting" id "and" protein-id)
      (cypher/merge-node-by-id! connection {:ref-id "p"
                                            :props  {:id protein-id}})
      (cypher/merge-node-by-id! connection {:ref-id "c"
                                            :props  {:id id}})
      (cypher/merge-rel! connection {:ref-id "r"
                                     :type   :references
                                     :from   {:props {:id protein-id}}
                                     :to     {:props {:id id}}}))))


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
