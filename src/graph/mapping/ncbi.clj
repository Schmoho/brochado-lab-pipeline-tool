(ns graph.mapping.ncbi
  (:require [graph.mapping.utils :refer :all]))

(defn organism-ref-id [id] (sanitize-ref-id (str "O_NCBI_" id)))
(defn assembly-ref-id [id] (sanitize-ref-id (str "A_NCBI_" id)))

(defn taxon-report->organism-node
  [{:keys [organism]}]
  (let [id (:tax_id organism)]
    {:ref-id (organism-ref-id id)
     :labels [:taxon :ncbi-taxon]
     :props  {:id        id
              :organism-name (:organism_name organism)}}))

(defn taxon-report->assembly-node
  [{:keys [assembly_info]}]
  (let [id (get-in assembly_info [:paired_assembly :accession])]
    {:ref-id (assembly-ref-id id)
     :labels [:assembly :ncbi-assembly]
     :props  (cond->
                 {:id              id
                  :assembly-name   (:assembly_name assembly_info)
                  :assembly-type   (:assembly_type assembly_info)
                  :assembly-status (get-in assembly_info [:paired_assembly :status])}
               (:refseq_category assembly_info) (assoc :refseq-category (:refseq_category assembly_info)))}))

(defn taxon-report->taxon-assembly-rel
  [report]
  (let [organism-id (get-in report [:organism :tax_id])
        assembly-id (get-in report [:assembly_info :paired_assembly :accession])]
    {:ref-id (str "REL_"
                  (organism-ref-id organism-id)
                  "__"
                  (assembly-ref-id assembly-id))
     :type  :has-assembly
     :from  {:ref-id (organism-ref-id organism-id)
             :labels [:taxon :ncbi-taxon]
             :props  {:id organism-id}}
     :to    {:ref-id (assembly-ref-id assembly-id)
             :labels [:assembly :ncbi-assembly]
             :props  {:id assembly-id}}
     :props {:submitter    (get-in report [:assembly_info :submitter])
             :release-date (get-in report [:assembly_info :release_date])}}))

(defn report->neo4j
  [report]
  (let [o-node   (taxon-report->organism-node report)
        entities {:nodes #{o-node
                           (taxon-report->assembly-node report)}
                  :rels  #{(taxon-report->taxon-assembly-rel report)
                           (rel-between
                            :contains
                            {:ref-id "D"
                             :labels [:database]
                             :props  {:id "NCBITaxonomy"}}
                            o-node)}}
        ref-ids  (->> entities
                     vals
                     (apply concat)
                     (map :ref-id)
                     set)]
    (-> (assoc entities :returns ref-ids)
        (sanitize-graph))))

#_(->> biodb.ncbi.api/taxon-report
       :reports
       (map (comp sanitize report->neo4j))
       (apply merge-with into))
