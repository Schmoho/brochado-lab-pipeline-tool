(ns graph.load.setup
  (:require
   [biodb.uniprot.api :as api.uniprot]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [graph.load.ncbi :as load.ncbi]
   [graph.load.presets :as presets]
   [graph.load.uniprot :as load.uniprot]
   [neo4clj.client :as client]))

(defn define-constraints!
  [connection]
  (doseq [constraint (concat load.uniprot/constraints
                             load.ncbi/constraints)]
    (client/execute! connection constraint)))

(defn delete-constraints!
  [connection]
  (->> (client/execute! connection "show constraints")
       (map #(get % "name"))
       (map #(client/execute! connection (str "drop constraint " %)))
       doall))

(defn load-databases!
  [connection]
  (doseq [database (concat presets/databases
                           (api.uniprot/databases))]
    (log/debug "Loading database node" (:abbrev database) ".")
    (load.uniprot/load-database! connection (update database :servers (fn [list-prop] (str "[" (str/join ","  list-prop) "]"))))))

#_(define-constraints! connection)
#_(delete-constraints! connection)
#_(load-databases! connection)
#_(user/clear-db! connection)


;; Stuff

#_(load-taxon! connection "208964")

#_(write-import-csvs! g)
#_(execute-import-csvs! g)

#_(client/execute! connection load.uniprot/associate-with-ncbi-taxons-query)

;; CREATE OR REPLACE DATABASE neo4j

#_(def g (load-taxon2! connection "208964"))
#_(cypher/merge-graph! connection (load-taxon2! connection "208964"))

#_(load.uniprot/load-protein! connection user/uniprot-protein)

#_(defn load-taxon!
  [connection taxon-id]
  (log/debug "Loading taxon" taxon-id "from NCBI.")
  (->> (api.ncbi/taxon-dataset-report taxon-id)
       (load.ncbi/load-taxon-report! connection))
  (log/debug "Loading taxon" taxon-id "from Uniprot.")
  (->> (api.uniprot/taxonomy-entry taxon-id)
       (load.uniprot/load-taxon! connection))
  (doseq [proteome (api.uniprot/proteomes-by-taxon-id taxon-id)]
    (log/debug "Loading proteome" (:id proteome) "from Uniprot Proteomes.")
    (load.uniprot/load-proteome! connection proteome)
    (let [proteins (api.uniprot/proteins-by-proteome (:id proteome))]
      #_(load.uniprot/load-proteins! connection proteins)
      (->> proteins
           (partition-all 100)
           (map #(future
                   (let [connection (client/connect "bolt://localhost:7687")]
                     (doseq [protein %]
                       (log/debug "Loading protein" (:primaryAccession protein) "from UniprotKB.")
                       (load.uniprot/load-protein! connection protein))
                     (client/disconnect connection))))
           doall
           (map deref)
           doall))))

;; (defn load-taxon2!
;;   [connection taxon-id]
;;   (let [ncbi-taxon        (->> (api.ncbi/taxon-dataset-report taxon-id)
;;                                :reports
;;                                (map mapping.ncbi/report->neo4j)
;;                                (apply merge-graphs))
;;         uniprot-taxon     (->> (api.uniprot/taxonomy-entry taxon-id)
;;                                (mapping.uniprot/taxon->neo4j))
;;         uniprot-proteomes (->> (api.uniprot/proteomes-by-taxon-id taxon-id)
;;                                (map mapping.uniprot/proteome->neo4j)
;;                                (apply merge-graphs))
;;         _                 (log/info "Done with proteomes.")
;;         uniprot-proteins  (->> uniprot-proteomes
;;                                :nodes
;;                                (map (fn [proteome]
;;                                       (api.uniprot/proteins-by-proteome (-> proteome :props :id))))
;;                                (apply concat)
;;                                (pmap mapping.uniprot/protein->neo4j)
;;                                (doall)
;;                                (apply merge-graphs))
;;         full-graph        (merge-graphs ncbi-taxon
;;                                         uniprot-taxon
;;                                         uniprot-proteomes
;;                                         uniprot-proteins)]
;;     full-graph))
