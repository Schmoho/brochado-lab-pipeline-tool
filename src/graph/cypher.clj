(ns graph.cypher
  (:require
   [clojure.spec.alpha :as s]
   [neo4clj.client :as client]
   [neo4clj.query-builder :as builder]
   [neo4clj.cypher :as cypher]
   [neo4clj.sanitize :as sanitize]
   [neo4clj.convert :as convert]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [camel-snake-kebab.core :as csk]))

(defn merge-node!
  [connection node]
  (client/execute!
   connection
   (format "MERGE %s RETURN %s"
           (cypher/node node)
           (:ref-id node))))

(defn merge-rel!
  [connection rel]
  (client/execute!
   connection
   (str/replace (builder/create-rel-query rel true)
                "CREATE"
                "MERGE")))


(defn set-block
  [node]
  (-> (:props node)
      (convert/hash-map->properties)
      (update-keys (fn [k]
                     (str (:ref-id node) "." k)))
      (->> (map #(str/join " = " %))
           (str/join ", "))))

(defn merge-node-by-id!
  [connection node]
  (when-let [id (-> node :props :id)]
    (let [ref-id       (:ref-id node)
          set-props-s  (set-block node)
          set-labels-s (if (:labels node)
                         (->> (map csk/->PascalCaseString (:labels node))
                              (cons ref-id)
                              (str/join ":")
                              (str ", "))
                         "")
          query        (format "MERGE %s ON MATCH SET %s%s ON CREATE SET %s%s RETURN %s"
                               (cypher/node {:ref-id ref-id
                                             :props  {:id id}})
                               set-props-s
                               set-labels-s
                               set-props-s
                               set-labels-s
                               ref-id)]
      (client/execute! connection query))))

;; (defn merge-rel-by-ids!
;;   [connection rel]
;;   (let [from-id (-> rel :from :props :id)
;;         to-id   (-> rel :to :props :id)]
;;     (let [ref-id       (:ref-id rel)
;;           set-props-s  (str ref-id " = " (apply str (rest (butlast (cypher/node {:props (:props node)})))))
;;           query        (format "MERGE %s RETURN %s"
;;                                (cypher/rel rel)
;;                                set-props-s
;;                                set-props-s
;;                                set-labels-s
;;                                ref-id)]
;;       (client/execute! connection query))))

;; (defn merge-rel!
;;   [connection rel]
;;   (when-not (client/find-rel connection rel)
;;     (client/create-rel! connection rel)))

#_(defn merge-graph!
  [connection {:keys [nodes rels returns]}]
  {:nodes (->> nodes (map (partial merge-node! connection)) doall)
   :rels  (->> rels
               (map (partial merge-rel! connection))
               (filter some?)
               doall)})

(defn create-merge-graph-query
  [graph]
  (-> (builder/create-graph-query graph)
      (str/replace "CREATE" "MERGE")))

(defn merge-graph!
  [connection graph]
  (client/execute! connection (create-merge-graph-query graph)))

(defn merge-node-with-rels-by-id!
  [connection graph]
  (doall
   (concat
    (for [node (:nodes graph)]
      (merge-node-by-id! connection node))
    (for [rel (:rels graph)]
      (merge-rel! connection rel)))))

(s/fdef merge-node-with-rels-by-id!
  :args (s/cat :connection :cypher/connection :graph :cypher/graph))

