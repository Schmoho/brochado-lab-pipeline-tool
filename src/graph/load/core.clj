(ns graph.load.core
  (:require
   [biodb.kegg.api :as api.kegg]
   [biodb.kegg.parser :as parser.kegg]
   [biodb.uniprot.api :as api.uniprot]
   [clojure.walk :as walk]
   [graph.load.kegg :as load.kegg]
   [graph.load.uniprot :as load.uniprot]
   [graph.load.utils :refer :all]
   [neo4clj.client :as client]
   [clojure.tools.logging :as log]))

(def connection
  (client/connect "bolt://localhost:7687"))

(def system
  (let [uniprot-taxon-loader-module                (make-download-and-feed-module
                                                    api.uniprot/taxonomy-entry
                                                    (partial load.uniprot/load-taxon! connection))
        uniprot-proteome-by-taxon-id-loader-module (make-download-and-feed-module
                                                    api.uniprot/proteomes-by-taxon-id
                                                    (partial load.uniprot/load-proteome! connection))
        uniprot-protein-loader-module              (make-distributor-module
                                                    (comp
                                                     (fn [protein]
                                                       {:protein          protein
                                                        :cross-references (:uniProtKBCrossReferences protein)})
                                                     api.uniprot/uniprotkb-entry)
                                                    [:protein :cross-references])
        cross-reference-sorter                     (make-distributor-module
                                                    #(-> (group-by :database %)
                                                         (update-vals (partial map :id))
                                                         (update-keys
                                                          {"KEGG"      :kegg
                                                           "Proteomes" :uniprot/proteomes}))
                                                    [:kegg :uniprot/proteomes]
                                                    {:bulk-mapping (constantly true)})
        kegg-cds-loader-module                     (make-download-and-feed-module
                                                    (comp parser.kegg/parse-kegg-get-result
                                                          api.kegg/get)
                                                    (partial load.kegg/load-cds! connection))]
    {:modules    {:uniprot/taxon             uniprot-taxon-loader-module
                  :uniprot/proteome-by-taxon uniprot-proteome-by-taxon-id-loader-module
                  :uniprot/protein           uniprot-protein-loader-module
                  :uniprot/cr-sorter         cross-reference-sorter
                  :kegg/cds                  kegg-cds-loader-module}
     :connectors {:uniprot-protein->cr-sorter
                  (make-connector (-> uniprot-protein-loader-module :channels :cross-references)
                                  (-> cross-reference-sorter :input-queue))
                  :cr-sorter->kegg
                  (make-connector (-> cross-reference-sorter :channels :kegg)
                                  (-> kegg-cds-loader-module :download-queue))}}))

(defn stop!
  [module]
  (walk/prewalk
   (fn [e]
     (if (= (type e) java.lang.Thread)
       (do (.interrupt e) e)
       e))
   module))

(defn status
  [module]
  (walk/prewalk
   (fn [e]
     (cond
       (= (type e) java.util.concurrent.LinkedBlockingQueue) {:size (count e)}
       (= (type e) java.lang.Thread) {:state (str (.getState e))}
       :else e))
   module))

#_(-> uniprot-protein-loader-module :input-queue (.offer "P02919"))

#_(status system)

#_(status uniprot-protein-loader-module)

#_(stop! uniprot-protein-loader-module)
