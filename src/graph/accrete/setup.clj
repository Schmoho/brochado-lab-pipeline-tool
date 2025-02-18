(ns graph.accrete.setup
  (:require
   [biodb.uniprot.api :as api.uniprot]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [graph.accrete.ncbi :as accrete.ncbi]
   [graph.accrete.presets :as presets]
   [graph.accrete.uniprot :as accrete.uniprot]
   [neo4clj.client :as client]))

(defn define-constraints!
  [connection]
  (doseq [constraint (concat accrete.uniprot/constraints
                             accrete.ncbi/constraints)]
    (client/execute! connection constraint)))

(defn delete-constraints!
  [connection]
  (->> (client/execute! connection "show constraints")
       (map #(get % "name"))
       (map #(client/execute! connection (str "drop constraint " %)))
       doall))

(defn accrete-databases!
  [connection]
  (doseq [database (concat presets/databases
                           (api.uniprot/databases))]
    (log/debug "Accreteing database node" (:abbrev database) ".")
    (accrete.uniprot/accrete-database! connection (update database :servers (fn [list-prop] (str "[" (str/join ","  list-prop) "]"))))))

#_(define-constraints! accrete.core/connection)
#_(delete-constraints! accrete.core/connection)

#_(do
    (user/clear-db! accrete.core/connection)
    (accrete-databases! accrete.core/connection))

;; Stuff

#_(accrete-taxon! connection "208964")

#_(write-import-csvs! g)
#_(execute-import-csvs! g)

#_(client/execute! connection accrete.uniprot/associate-with-ncbi-taxons-query)

;; CREATE OR REPLACE DATABASE neo4j

#_(def g (accrete-taxon2! connection "208964"))
#_(cypher/merge-graph! connection (accrete-taxon2! connection "208964"))

#_(accrete.uniprot/accrete-protein! connection user/uniprot-protein)

#_(defn accrete-taxon!
  [connection taxon-id]
  (log/debug "Accreteing taxon" taxon-id "from NCBI.")
  (->> (api.ncbi/taxon-dataset-report taxon-id)
       (accrete.ncbi/accrete-taxon-report! connection))
  (log/debug "Accreteing taxon" taxon-id "from Uniprot.")
  (->> (api.uniprot/taxonomy-entry taxon-id)
       (accrete.uniprot/accrete-taxon! connection))
  (doseq [proteome (api.uniprot/proteomes-by-taxon-id taxon-id)]
    (log/debug "Accreteing proteome" (:id proteome) "from Uniprot Proteomes.")
    (accrete.uniprot/accrete-proteome! connection proteome)
    (let [proteins (api.uniprot/proteins-by-proteome (:id proteome))]
      #_(accrete.uniprot/accrete-proteins! connection proteins)
      (->> proteins
           (partition-all 100)
           (map #(future
                   (let [connection (client/connect "bolt://localhost:7687")]
                     (doseq [protein %]
                       (log/debug "Accreteing protein" (:primaryAccession protein) "from UniprotKB.")
                       (accrete.uniprot/accrete-protein! connection protein))
                     (client/disconnect connection))))
           doall
           (map deref)
           doall))))

;; (defn accrete-taxon2!
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
