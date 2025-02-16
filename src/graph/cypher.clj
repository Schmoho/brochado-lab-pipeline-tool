(ns graph.cypher
  (:require
   [neo4clj.client :as client]
   [neo4clj.query-builder :as builder]
   [neo4clj.cypher :as cypher]
   [clojure.string :as str]))

(defn merge-node!
  [connection node]
  (client/execute!
   connection
   (format "MERGE %s RETURN %s"
           (cypher/node node)
           (:ref-id node))))

(defn merge-rel!
  [connection rel]
  (when-not (client/find-rel connection rel)
    (client/create-rel! connection rel)))

#_(defn merge-graph!
  [connection {:keys [nodes rels returns]}]
  {:nodes (->> nodes (map (partial merge-node! connection)) doall)
   :rels  (->> rels
               (map (partial merge-rel! connection))
               (filter some?)
               doall)})

(defn match-create-rel!
  [connection rel]
  (client/execute!
   connection
   ))


(defn create-merge-graph-query
  [graph]
  (-> (builder/create-graph-query graph)
      (str/replace "CREATE" "MERGE")))

(defn merge-graph!
  [connection graph]
  (client/execute! connection (create-merge-graph-query graph)))
