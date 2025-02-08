(ns db.load.uniprot
  (:require
   [db.load.cypher :as cypher]
   [clojure.string :as str]
   [db.load.utils :refer :all]))

(defn taxon->taxon-node
  [taxon]
  {:ref-id "taxon"
   :labels [:taxon :uniprot]
   :props  {:tax-id          (:taxonId taxon)
            :scientific-name (:scientificName taxon)
            :rank            (:rank taxon)
            :proteome-count (get-in taxon [:statistics :proteomeCount])}})

(defn proteome->proteome-node
  [proteome]
  {:ref-id "proteome"
   :labels [:proteome :uniprot]
   :props  (merge
            (select-keys proteome [:proteinCount
                                   :strain
                                   :modified
                                   :proteomeType])
            {:proteome-id             (:id proteome)}
            (:proteomeStatistics proteome)
            (when-let [redundant-to-proteome-id (:redundantTo proteome)]
              {:redundant-to-proteome-id redundant-to-proteome-id}))})

(def proteome->taxon-relation
  (comp
   (fn [[proteome-id tax-id]]
     [{:type  :has-proteome
       :from  {:ref-id "taxon"
               :labels [:taxon :uniprot]
               :props  {:tax-id tax-id}}
       :to    {:ref-id "proteome"
               :labels [:proteome :uniprot]
               :props {:proteome-id proteome-id}}}])
   (juxt :id (comp :taxonId :taxonomy))))

(def proteome->assembly-relation
  (comp
   (fn [[proteome-id accession]]
     {:type  :corresponds
      :from  {:ref-id "proteome"
              :labels [:proteome :uniprot]
              :props  {:proteome-id proteome-id}}
      :to    {:ref-id "assembly"
              :labels [:assembly :ncbi]
              :props {:accession accession}}})
   (juxt :id (comp :assemblyId :genomeAssembly))))

(defn protein->protein-node
  [protein]
  {:ref-id "protein"
   :labels [:protein :uniprot]
   :props  (merge
            (select-keys
             protein
             [:proteinExistence
              :entryType
              :uniProtkbId])
            {:accession       (:primaryAccession protein)
             :sequence        (-> protein :sequence :value)
             :sequence-weight (-> protein :sequence :molWeight)}
            (when-let [recommended-name (some->
                                         protein
                                         :proteinDescription
                                         :recommendedName
                                         :fullName
                                         :value
                                         escape-backticks)]
              {:recommended-name recommended-name})
            (when-let [recommended-short-name (some->
                                               protein
                                               :proteinDescription
                                               :recommendedName
                                               :shortName
                                               :value
                                               escape-backticks)]
              {:recommended-short-name recommended-short-name}))})


(defn load-taxon!
  [connection taxon]
  (cypher/merge-node! connection (taxon->taxon-node taxon)))

(defn load-proteome!
  [connection proteome]
  (cypher/merge-node! connection (proteome->proteome-node proteome)))

(defn load-protein!
  [connection protein]
  (tap> protein)
  (cypher/merge-node! connection (protein->protein-node protein)))

(def associate-with-ncbi-taxons-query
  "MATCH (t:Taxon)
WITH t.taxId AS sharedTaxonId, collect(t) AS taxonGroup
UNWIND taxonGroup AS t1
UNWIND taxonGroup AS t2
WITH t1, t2
WHERE id(t1) < id(t2)
MERGE (t1)-[:CORRESPONDS_EXACTLY]->(t2)
MERGE (t1)<-[:CORRESPONDS_EXACTLY]-(t2)")
