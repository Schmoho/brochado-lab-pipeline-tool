(ns biodb.uniprot.blast
  (:import (uk.ac.ebi.uniprot.dataservice.client ServiceFactory Client)
           (uk.ac.ebi.uniprot.dataservice.client.uniprot UniProtService)
           (uk.ac.ebi.uniprot.dataservice.client.alignment.blast BlastInput BlastInput$Builder BlastService BlastResult)
           (uk.ac.ebi.uniprot.dataservice.client.alignment.blast.input DatabaseOption)))

(defn alignment->clj
  [alignment]
  {:score                (.getScore alignment)
   :smith-waterman-score (.getSmithWatermanScore alignment)
   :z-score              (.getZScore alignment)
   :positives            (.getPositives alignment)
   :gaps                 (.getGaps alignment)
   :identity             (.getIdentity alignment)
   :probability          (.getProbability alignment)
   :overlap              (.getOverlap alignment)
   :strand               (.getStrand alignment)
   :query-seq            (.getQuerySeq alignment)
   :match-seq            (.getMatchSeq alignment)
   :start-query-seq      (.getStartQuerySeq alignment)
   :end-query-seq        (.getEndQuerySeq alignment)
   :start-match-seq      (.getStartMatchSeq alignment)
   :end-match-seq        (.getEndMatchSeq alignment)
   :ungaps               (.getUngaps alignment)
   :bit-score            (.getBitScore alignment)
   :expectation          (.getExpectation alignment)
   :pattern              (.getPattern alignment)})

(defn summary->clj
  [summary]
  {:id              (.getEntryAc summary)
   :isoform?        (.isIsoform summary)
   :hit-number      (.getHitNumber summary)
   :entry-id        (.getEntryId summary)
   :sequence-length (.getSequenceLength summary)
   :alignments      (map alignment->clj (.getAlignments summary))
   :database        (.getDatabase summary)
   :description     (.getDescription summary)})

(defn blast-result->clj
  [^BlastResult blast-result]
  (->> (.hits blast-result)
       (map (comp summary->clj #(.getSummary %)))
       (sort-by :hit-number)))

(def database->enum
  {:swissprot                            DatabaseOption/SWISSPROT
   :trembl                               DatabaseOption/TREMBL
   :uniparc                              DatabaseOption/UNIPARC
   :uniprot-archaea                      DatabaseOption/UNIPROT_ARCHAEA
   :uniprot-arthropoda                   DatabaseOption/UNIPROT_ARTHROPODA
   :uniprot-bacteria                     DatabaseOption/UNIPROT_BACTERIA
   :uniprot-complete-microbial-proteomes DatabaseOption/UNIPROT_COMPLETE_MICROBIAL_PROTEOMES
   :uniprot-eukaryota                    DatabaseOption/UNIPROT_EUKARYOTA
   :uniprot-fungi                        DatabaseOption/UNIPROT_FUNGI
   :uniprot-human                        DatabaseOption/UNIPROT_HUMAN
   :uniprot-mammals                      DatabaseOption/UNIPROT_MAMMALS
   :uniprot-nematoda                     DatabaseOption/UNIPROT_NEMATODA
   :uniprot-pdb                          DatabaseOption/UNIPROT_PDB
   :uniprot-rodents                      DatabaseOption/UNIPROT_RODENTS
   :uniprot-vertebrates                  DatabaseOption/UNIPROT_VERTEBRATES
   :uniprot-viridiplantae                DatabaseOption/UNIPROT_VIRIDIPLANTAE
   :uniprot-viruses                      DatabaseOption/UNIPROT_VIRUSES
   :uniprotkb                            DatabaseOption/UNIPROTKB
   :uniref-100                           DatabaseOption/UNIREF_100
   :uniref-50                            DatabaseOption/UNIREF_50
   :uniref-90                            DatabaseOption/UNIREF_90})

(defn run-blast-query!
  "Returns a future."
  [{:keys [blast/database blast/query-sequence]}]
  (let [blast-service (doto (.getUniProtBlastService
                             (Client/getServiceFactoryInstance))
                        (.start ))
        input         (-> (BlastInput$Builder.
                           (database->enum database)
                           query-sequence)
                          (.build))
        result        (.runBlast blast-service input)]
    (.stop blast-service)
    (future (blast-result->clj @result))))



;; (cypher/merge-graph!
;;  graph.accrete.core/connection
;;  q(mapping.uniprot.blast/blast-result->blast-graph
;;   pa-mrcb
;;   {:blast/database       :uniprot-bacteria
;;    :blast/query-sequence mrcB}
;;   b))


;; (cypher/create-merge-graph-query
;;  (mapping.uniprot.blast/blast-result->blast-graph
;;   pa-mrcb
;;   {:blast/database       :uniprot-bacteria
;;    :blast/query-sequence mrcB}
;;   b))

