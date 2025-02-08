(ns db.load.core
  (:require
   [neo4clj.client :as client]
   [biodb.ncbi :as ncbi]
   [biodb.uniprot :as uniprot]
   [db.load.cypher :as cypher]
   [db.load.ncbi :as load.ncbi]
   [db.load.uniprot :as load.uniprot]))

(def connection
  (client/connect "bolt://localhost:7687"))

(def nacbi-taxes ["208963" "208964" "652611"])

(defn load-ncbi!
  [connection taxon-ids]
  (doseq [taxon-id taxon-ids]
    (load.ncbi/load-taxons! connection (ncbi/taxon-dataset-report taxon-id))
    (load.ncbi/load-assemblies! connection (ncbi/taxon-dataset-report taxon-id))))


(defn load-uniprot!
  [connection]
  (let [taxon-ids (client/find-nodes connection {:ref-id "p" :labels [:ncbi :taxon]})]
    (->> taxon-ids
         (map (comp (partial load.uniprot/load-taxon! connection)
                    uniprot/taxonomy-entry
                    :tax-id
                    :props))
         doall)
    (client/execute! connection load.uniprot/associate-with-ncbi-taxons-query)
    (->> taxon-ids
         (map (comp uniprot/proteomes-by-taxon-id :tax-id :props))
         (apply concat)
         (map (partial load.uniprot/load-proteome! connection))
         doall)
    (->> taxon-ids
         (map (comp uniprot/proteomes-by-taxon-id :tax-id :props))
         (apply concat)
         (map (juxt load.uniprot/proteome->taxon-relation
                    load.uniprot/proteome->assembly-relation))
         flatten
         (map (partial cypher/merge-rel! connection))
         doall)
    #_(->> (client/find-nodes connection {:ref-id "p" :labels [:uniprot :proteome]})
         (map (comp :proteome-id :props))
         (map uniprot/proteins-by-proteome)
         (apply concat)
         (map (partial load.uniprot/load-protein! connection))
         doall)))

#_(load-ncbi! connection nacbi-taxes)
#_(load-uniprot! connection)

;; taxonomic lineage
;; genes aus proteinen

(def insane-taxonomic-levels
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

(doseq [taxon-id (->> (client/find-nodes connection {:ref-id "p" :labels [:uniprot :taxon]})
                      (map (comp
                            uniprot/taxonomy-entry
                            :tax-id
                            :props))
                      (map :lineage)
                      (apply concat)
                      (filter #((complement insane-taxonomic-levels) (:rank %)))
                      (map :taxonId)
                      distinct)]
  (load.ncbi/load-taxons! connection (ncbi/taxon-dataset-report taxon-id)))



(defn clear-db!
  [connection]
  (do
    (client/delete-rel! connection {:type :has-assembly})
    (client/delete-rel! connection {:type :corresponds-exactly})
    (client/delete-node! connection {:labels [:ncbi :taxon]})
    (client/delete-node! connection {:labels [:assembly]})))

;; (load-ncbi! connection)
;; (load-uniprot! connection)

#_(client/disconnect connection)

;; ;; proteome completeness reports
