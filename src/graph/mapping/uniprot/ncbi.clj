(ns graph.mapping.uniprot.ncbi
  (:require
   [graph.mapping.ncbi :as ncbi]
   [graph.mapping.utils :refer :all]))

(defn proteome-ref-id [id] (sanitize-ref-id (str "PP_UNIPROT_" id)))

(defn proteome->assembly-relation
  [proteome]
  (let [assembly-id (-> proteome :genomeAssembly :assemblyId)
        proteome-id (-> proteome :id)]
    (when assembly-id
      {:type   :has-assembly
       :ref-id (str "REL_"
                    (proteome-ref-id proteome-id)
                    "__"
                    (ncbi/assembly-ref-id assembly-id))
       :from   {:ref-id (proteome-ref-id proteome-id)
                :labels [:proteome :uniprot]
                :props  {:id proteome-id}}
       :to     {:ref-id (ncbi/assembly-ref-id assembly-id)
                :labels [:assembly :ncbi]
                :props  {:id assembly-id}}})))


(def associate-with-ncbi-taxons-query
  "MATCH (t:Taxon)
WITH t.taxId AS sharedTaxonId, collect(t) AS taxonGroup
UNWIND taxonGroup AS t1
UNWIND taxonGroup AS t2
WITH t1, t2
WHERE id(t1) < id(t2)
MERGE (t1)-[:CORRESPONDS_EXACTLY]->(t2)
MERGE (t1)<-[:CORRESPONDS_EXACTLY]-(t2)")
