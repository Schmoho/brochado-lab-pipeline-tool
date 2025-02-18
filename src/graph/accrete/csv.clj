(ns graph.accrete.csv
    (:require
   [biodb.ncbi.api :as api.ncbi]
   [biodb.uniprot.api :as api.uniprot]
   [camel-snake-kebab.core :as csk]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.accrete.core :as accrete]
   [graph.accrete.ncbi :as accrete.ncbi]
   [graph.accrete.presets :as presets]
   [graph.accrete.uniprot :as accrete.uniprot]
   [graph.mapping.ncbi :as mapping.ncbi]
   [graph.mapping.uniprot.core :as mapping.uniprot]
   [graph.mapping.utils :refer :all]
   [neo4clj.client :as client]
   [neo4clj.query-builder :as builder]))


(defn write-import-csvs!
  [g]
  (let [grouped (->> g :nodes (group-by :labels))]
   (for [group (keys grouped)]
     (let [file-name (str (->> group
                               (map (comp csk/->PascalCase name))
                               (str/join "__" ))
                          ".csv")
           file-path (str "test/" file-name)
           entities  (get grouped group)
           header    (-> entities
                         first
                         :props
                         (update-keys name)
                         sort
                         keys
                         (->> (map (comp (fn [s]
                                           (str (str/lower-case (subs s 0 1))
                                                (subs s 1)))
                                         csk/->PascalCase))))]
       (with-open [writer (io/writer file-path)]
         (csv/write-csv
          writer
          (cons header
                (->> entities (map (comp vals sort :props))))
          :quote? (constantly true)))))))

(defn execute-import-csvs!
  [connection g]
  (let [grouped (->> g :nodes (group-by :labels))]
   (for [group (keys grouped)]
     (let [file-name (str (->> group
                               (map (comp csk/->PascalCase name))
                               (str/join "__" ))
                          ".csv")
           file-path (str "test/" file-name)
           entities  (get grouped group)
           header    (-> entities
                         first
                         :props
                         (update-keys name)
                         sort
                         keys
                         (->> (map (comp (fn [s]
                                           (str (str/lower-case (subs s 0 1))
                                                (subs s 1)))
                                         csk/->PascalCase))))]
       (let [labels-string     (->> group
                                    (map (comp csk/->PascalCase name))
                                    (str/join ":" ))
             properties-string (str "{"
                                    (str/join ", "
                                              (map #(str % ": row." % ) header))
                                    "}")]
         (client/execute! connection (format "LOAD CSV WITH HEADERS FROM 'file:///%s' AS row
CALL {WITH row MERGE (c:%s %s) ON MATCH SET c.id = row.id} IN TRANSACTIONS OF 100000 ROWS;"
                                             file-name
                                             labels-string
                                             properties-string)))))))
