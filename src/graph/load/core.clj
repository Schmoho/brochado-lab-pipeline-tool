(ns graph.load.core
  (:require
   [graph.spec]
   [biodb.kegg.api :as api.kegg]
   [biodb.kegg.parser :as parser.kegg]
   [biodb.uniprot.api :as api.uniprot]
   [clojure.walk :as walk]
   [graph.load.kegg :as load.kegg]
   [graph.load.uniprot :as load.uniprot]
   [channels :as channels]
   [neo4clj.client :as client]
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.load.setup :as setup]
   [clojure.core.async :as a]
   [graph.mapping.uniprot.core :as mapping.uniprot])
  (:import (java.util.concurrent LinkedBlockingQueue)))

(def connection
  (client/connect "bolt://localhost:7687"))

(def changes (atom {}))

(defn register-expectation!
  [type id expectation-promise]
  (swap! changes #(update % [type id] conj expectation-promise)))

(defn register-change!
  [{:keys [input-data db-result type id] :as result}]
  (log/info "Registering result for" [type id])
  (let [listener (get @changes [type id])]
    (log/info "Found listener" listener)
    (swap! changes #(dissoc % [type id]))
    (doseq [l listener]
      (deliver l result))))

(defn make-system!
  []
  (let [uniprot-protein-api              (channels/make-distributor-module!
                                          {:operation     (comp
                                                           (fn [protein]
                                                             {:protein          protein
                                                              :cross-references protein})
                                                           api.uniprot/uniprotkb-entry)
                                           :channel-names [:protein :cross-references]})
        uniprot-protein-db-load          (channels/make-linear-module!
                                          {:operation     (partial load.uniprot/load-protein! connection)
                                           :input-channel (-> uniprot-protein-api :channels :protein)})
        cross-reference-sorter           (channels/make-distributor-module!
                                          {:operation     (fn [protein]
                                                            (load.uniprot/connect-protein! connection protein)
                                                            (mapping.uniprot/protein->cross-ids protein))
                                           :channel-names [:kegg :uniprot/proteomes :uniprot/taxonomy]
                                           :input-channel (-> uniprot-protein-api :channels :cross-references)
                                           :bulk-mapping  (constantly true)})
        uniprot-taxon-api                (channels/make-linear-module!
                                          {:operation     api.uniprot/taxonomy-entry
                                           :input-channel (-> cross-reference-sorter :channels :uniprot/taxonomy)})
        uniprot-taxon-db-load            (channels/make-linear-module!
                                          {:operation     (partial load.uniprot/load-taxon! connection)
                                           :input-channel (:output-channel uniprot-taxon-api)})
        uniprot-proteome-by-taxon-id-api (channels/make-linear-module!
                                          {:operation api.uniprot/proteomes-by-taxon-id})
        uniprot-proteome-db-load         (channels/make-linear-module!
                                          {:operation     (partial load.uniprot/load-proteome! connection)
                                           :input-channel (:output-channel uniprot-proteome-by-taxon-id-api)})
        kegg-cds-api                     (channels/make-linear-module!
                                          {:operation     (comp parser.kegg/parse-kegg-get-result
                                                                api.kegg/get)
                                           :input-channel (-> cross-reference-sorter :channels :kegg)})
        kegg-cds-db-load                 (channels/make-linear-module!
                                          {:operation     (partial load.kegg/load-cds! connection)
                                           :input-channel (:output-channel kegg-cds-api)})
        new-entities-funnel              (channels/make-funnel! {:from [(:output-channel uniprot-taxon-db-load)
                                                                        (:output-channel uniprot-protein-db-load)
                                                                        (:output-channel kegg-cds-db-load)]})
        change-registry                  (channels/make-sink! {:input-channel (:to new-entities-funnel)
                                                               :operation     register-change!})
        entrypoint                       (channels/make-distributor-module!
                                          {:operation     (fn [stuff]
                                                            (case (:requested-accretion-type stuff)
                                                              :uniprot/taxon
                                                              {:uniprot/taxon-ids (:id stuff)}
                                                              :uniprot/protein
                                                              {:uniprot/protein-ids (:id stuff)}))
                                           :channel-names [:uniprot/taxon-ids :uniprot/protein-ids]})
        _                                (channels/pipe! {:from (-> entrypoint :channels :uniprot/taxon-ids)
                                                          :to   (-> uniprot-taxon-api :input-channel)})
        _                                (channels/pipe! {:from (-> entrypoint :channels :uniprot/protein-ids)
                                                          :to   (-> uniprot-protein-api :input-channel)})]
    {:uniprot/taxon-api                uniprot-taxon-api
     :uniprot/taxon-db-load            uniprot-taxon-db-load
     :uniprot/proteome-by-taxon-id-api uniprot-proteome-by-taxon-id-api
     :uniprot/protein-api              uniprot-protein-api
     :uniprot/protein-db-load          uniprot-protein-db-load
     :uniprot/cross-reference-sorter   cross-reference-sorter
     :kegg/cds-api                     kegg-cds-api
     :kegg/cds-db-load                 kegg-cds-db-load
     :new-entities-funnel              new-entities-funnel
     :entrypoint                       entrypoint
     #_#_:change-registry              change-registry}))


(defn status
  [system]
  (walk/prewalk
   (fn [e]
     (cond
       (= (type e) java.util.concurrent.LinkedBlockingQueue) {:size (count e)}
       (= (type e) java.lang.Thread) {:state (str (.getState e))}
       :else e))
   system))

(defn stop!
  [system-atom]
  (log/info "Terminating accretion system threads.")
  (walk/prewalk
   (fn [e]
     (if (= (type e) java.lang.Thread)
       (do (.interrupt e) e)
       e))
   @system-atom))

(defn start!
  [system-atom]
  (log/info "Starting accretion system.")
  (reset! system-atom (make-system!)))

(defn restart!
  [system-atom]
  (log/info "Restarting accretion system.")
  (stop! system-atom)
  (start! system-atom))

(defonce system (atom {}))

(defn submit!
  [stuff]
  (if-let [in (-> @system :entrypoint :input-channel)]
    (.offer in stuff)
    (log/error "Accretion system is not running.")))


#_(-> @system :uniprot/protein-db-load :output-channel)
#_(-> @system :new-entities-funnel :to (.peek))

;; (def s (make-thing-doer! (LinkedBlockingQueue.)
;;                          (LinkedBlockingQueue.)
;;                          identity))

;; (def s (for [i (range 20)]
;;          (make-sink! {:operation identity
;;                      :input-channel (LinkedBlockingQueue.)})))

;; (map #(.getState %) s)

#_(status @system)

#_(do
    (user/clear-db! connection)
    (setup/load-databases! connection)
    (restart! system))

#_(-> @system :uniprot/protein-api :input-channel (.offer "P02919"))
