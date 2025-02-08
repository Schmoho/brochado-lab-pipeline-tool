(ns db.load.ncbi
  (:require
   [db.load.cypher :as cypher]
   [db.load.utils :refer :all]))

(defn taxon-report->organism-node
  [{:keys [organism]}]
  {:ref-id "t"
   :labels [:taxon
            :ncbi]
   :props  (merge {:tax-id        (:tax_id organism)
                   :organism-name (:organism_name organism)})})

(defn taxon-report->assembly-node
  [{:keys [assembly_info]}]
  {:labels [:assembly :ncbi]
   :props  (cond->
               {:assembly-name   (:assembly_name assembly_info)
                :assembly-type   (:assembly_type assembly_info)
                :accession       (get-in assembly_info [:paired_assembly :accession])
                :assembly-status (get-in assembly_info [:paired_assembly :status])}
             (:refseq_category assembly_info) (assoc :refseq-category (:refseq_category assembly_info)))})

(defn taxon-report->taxon-assembly-rel
  [report]
  {:type  :has-assembly
   :from  {:ref-id "t"
           :labels [:taxon :ncbi]
           :props  {:tax-id (get-in report [:organism :tax_id])}}
   :to    {:ref-id "a"
           :labels [:assembly :ncbi]
           :props {:accession (get-in report [:assembly_info :paired_assembly :accession])}}
   :props {:submitter    (escape-backticks (get-in report [:assembly_info :submitter]))
           :release-date (get-in report [:assembly_info :release_date])}})

(defn load-taxons!
  [connection taxon-report]
  (let [reports (:reports taxon-report)]
    (->> reports
         (map taxon-report->organism-node)
         distinct
         (map (partial cypher/merge-node! connection))
         (doall))))

(defn load-assemblies!
  [connection taxon-report]
  (let [reports (:reports taxon-report)]
    (->> reports
         (map taxon-report->assembly-node)
         distinct
         (map (partial cypher/merge-node! connection))
         (doall))
    (->> reports
         (map taxon-report->taxon-assembly-rel)
         (map (partial cypher/merge-rel! connection))
         (doall))))
