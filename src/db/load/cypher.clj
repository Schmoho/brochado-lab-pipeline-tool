(ns db.load.cypher
  (:require
   [neo4clj.client :as client]
   [neo4clj.cypher :as cypher]))

(defn merge-node!
  [connection node]
  (client/execute!
   connection
   (format "MERGE %s RETURN %s"
           (cypher/node node)
           (:ref-id node))))

(defn merge-rel!
  [connection rel]
  (when-not (client/find-rel
             connection
             (assoc rel :ref-id (or (:ref-id rel) "p")))
    (client/create-rel! connection rel)))
