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
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.load.setup :as setup]))

(def connection
  (client/connect "bolt://localhost:7687"))

(defn make-system!
  []
  (let [uniprot-protein-api              (make-distributor-module!
                                          {:operation     (comp
                                                           (fn [protein]
                                                             {:protein          protein
                                                              :cross-references protein})
                                                           api.uniprot/uniprotkb-entry)
                                           :channel-names [:protein :cross-references]})
        uniprot-protein-db-load          (make-linear-module!
                                          {:operation     (partial load.uniprot/load-protein! connection)
                                           :input-channel (-> uniprot-protein-api :channels :protein)})
        cross-reference-sorter           (make-distributor-module!
                                          {:operation     (fn [protein]
                                                            (let [protein-id       (:primaryAccession protein)
                                                                  cross-references (:uniProtKBCrossReferences protein)
                                                                  cross-ids        (-> (group-by :database cross-references)
                                                                                       (update-vals (partial map :id))
                                                                                       (update-keys
                                                                                        {"KEGG"      :kegg
                                                                                         "Proteomes" :uniprot/proteomes})
                                                                                       (assoc :uniprot/taxonomy [(-> protein :organism :taxonId)])
                                                                                       (dissoc nil))]
                                                              (doseq [[db ids] cross-ids
                                                                      id       ids]
                                                                (log/info "connecting" id "and" protein-id)
                                                                (cypher/merge-node-by-id! connection {:ref-id "p"
                                                                                                      :props  {:id protein-id}})
                                                                (cypher/merge-node-by-id! connection {:ref-id "c"
                                                                                                      :props  {:id id}})
                                                                (cypher/merge-rel! connection {:ref-id "r"
                                                                                               :type   :references
                                                                                               :from   {:props {:id protein-id}}
                                                                                               :to     {:props {:id id}}}))
                                                              cross-ids))
                                           :channel-names [:kegg :uniprot/proteomes :uniprot/taxonomy]
                                           :input-channel (-> uniprot-protein-api :channels :cross-references)
                                           :bulk-mapping  (constantly true)})
        uniprot-taxon-api                (make-linear-module!
                                          {:operation     api.uniprot/taxonomy-entry
                                           :input-channel (-> cross-reference-sorter :channels :uniprot/taxonomy)})
        uniprot-taxon-db-load            (make-linear-module!
                                          {:operation     (partial load.uniprot/load-taxon! connection)
                                           :input-channel (:output-channel uniprot-taxon-api)})
        uniprot-proteome-by-taxon-id-api (make-linear-module!
                                          {:operation api.uniprot/proteomes-by-taxon-id})
        uniprot-proteome-db-load         (make-linear-module!
                                          {:operation     (partial load.uniprot/load-proteome! connection)
                                           :input-channel (:output-channel uniprot-proteome-by-taxon-id-api)})
        kegg-cds-api                     (make-linear-module!
                                          {:operation     (comp parser.kegg/parse-kegg-get-result
                                                                api.kegg/get)
                                           :input-channel (-> cross-reference-sorter :channels :kegg)})
        kegg-cds-db-load                 (make-linear-module!
                                          {:operation     (partial load.kegg/load-cds! connection)
                                           :input-channel (:output-channel kegg-cds-api)})
        new-entities-funnel              (make-funnel! {:from [(:output-channel uniprot-protein-db-load)
                                                               (:output-channel kegg-cds-db-load)]})]
    {:uniprot/taxon-api                uniprot-taxon-api
     :uniprot/taxon-db-load            uniprot-taxon-db-load
     :uniprot/proteome-by-taxon-id-api uniprot-proteome-by-taxon-id-api
     :uniprot/protein-api              uniprot-protein-api
     :uniprot/protein-db-load          uniprot-protein-db-load
     :uniprot/cross-reference-sorter   cross-reference-sorter
     :kegg/cds-api                     kegg-cds-api
     :kegg/cds-db-load                 kegg-cds-db-load
     :new-entities-funnel              new-entities-funnel}))

(defn stop!
  [system]
  (walk/prewalk
   (fn [e]
     (if (= (type e) java.lang.Thread)
       (do (.interrupt e) e)
       e))
   system))

(defn status
  [system]
  (walk/prewalk
   (fn [e]
     (cond
       (= (type e) java.util.concurrent.LinkedBlockingQueue) {:size (count e)}
       (= (type e) java.lang.Thread) {:state (str (.getState e))}
       :else e))
   system))

(def system (atom {}))

(defn restart!
  []
  (stop! @system)
  (reset! system (make-system!)))




#_(-> @system :uniprot/protein-db-load :output-channel)
#_(-> @system :new-entities-funnel :to (.peek))

#_(status @system)

#_(do
    (user/clear-db! connection)
    (setup/load-databases! connection)
    (restart!))

#_(-> @system :uniprot/protein-api :input-channel (.offer "P02919"))
