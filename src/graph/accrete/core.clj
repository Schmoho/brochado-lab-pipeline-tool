(ns graph.accrete.core
  (:require
   [graph.spec]
   [biodb.kegg.api :as api.kegg]
   [biodb.kegg.parser :as parser.kegg]
   [biodb.uniprot.api :as api.uniprot]
   [clojure.walk :as walk]
   [graph.accrete.kegg :as accrete.kegg]
   [graph.accrete.uniprot :as accrete.uniprot]
   [channels :as channels]
   [neo4clj.client :as client]
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.accrete.setup :as setup]
   [clojure.core.async :as a]
   [graph.mapping.uniprot.core :as mapping.uniprot]
   [biodb.uniprot.core :as uniprot])
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
    (swap! changes #(dissoc % [type id]))
    (doseq [l listener]
      (deliver l result))))

(defn make-system-2!
  []
  (let [uniprot-protein-api              (a/chan 100 (map api.uniprot/uniprotkb-entry))
        uniprot-protein-api-mult         (a/mult uniprot-protein-api)
        uniprot-protein-db-accrete       (->> (a/chan 100 (map (partial accrete.uniprot/accrete-protein! connection)))
                                              (a/tap uniprot-protein-api-mult))
        cross-references                 (as-> (a/chan 100 (mapcat :uniProtKBCrossReferences)) $
                                           (a/tap uniprot-protein-api-mult $)
                                           (a/pub $ :database))
        uniprot-taxon-api                (a/chan 100 (map api.uniprot/taxonomy-entry))
        protein|taxon                    (as-> (a/chan 100 (map (comp :taxonId :organism))) $
                                           (a/tap uniprot-protein-api-mult $)
                                           (a/pipe $ uniprot-taxon-api))
        uniprot-taxon-db-accrete         (a/chan 100 (map (partial accrete.uniprot/accrete-taxon! connection)))
        uniprot-proteome-by-taxon-id-api (a/chan 100 (map api.uniprot/proteomes-by-taxon-id))
        uniprot-proteome-db-accrete      (a/chan 100 (map (partial accrete.uniprot/accrete-proteome! connection)))
        kegg-cds-api                     (a/chan 100 (map (comp parser.kegg/parse-kegg-get-result
                                                                api.kegg/get)))
        kegg-cds-db-accrete              (a/chan 100 (map (partial accrete.kegg/accrete-cds! connection)))
        entrypoint                       (a/chan)
        distributor                      (a/pub entrypoint :requested-accretion-type)]
    (do
      (let [c (a/chan 100 (map :id))] (a/sub distributor :uniprot/taxon c) (a/pipe c uniprot-taxon-api))
      (let [c (a/chan 100 (map :id))] (a/sub distributor :uniprot/protein c) (a/pipe c uniprot-protein-api))
      (let [c (a/chan 100 (map :id))] (a/sub cross-references "KEGG" c) (a/pipe c kegg-cds-api))
      (a/pipe kegg-cds-api kegg-cds-db-accrete)
      (a/pipe uniprot-taxon-api uniprot-taxon-db-accrete))
    {:go-blocks  {:change-registry (a/go-loop []
                                     (let [[change channel]
                                           (a/alts! [uniprot-protein-db-accrete
                                                     uniprot-taxon-db-accrete
                                                     kegg-cds-db-accrete])]
                                       (register-change! change))
                                     (recur))}
     :channels   {:uniprot/protein-api              uniprot-protein-api
                  :uniprot/protein-api-mult         uniprot-protein-api-mult
                  :uniprot/protein-db-accrete       uniprot-protein-db-accrete
                  :uniprot/cross-references         cross-references
                  :uniprot/taxon-api                uniprot-taxon-api
                  :uniprot/taxon-db-accrete         uniprot-taxon-db-accrete
                  :uniprot/protein|taxon            protein|taxon
                  :uniprot/proteome-by-taxon-id-api uniprot-proteome-by-taxon-id-api
                  :uniprot/proteome-db-accrete      uniprot-proteome-db-accrete
                  :kegg/cds-api                     kegg-cds-api
                  :kegg/cds-db-accrete              kegg-cds-db-accrete}     
     :entrypoint entrypoint}))

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
  (reset! system-atom (make-system-2!)))

(defn restart!
  [system-atom]
  (log/info "Restarting accretion system.")
  (stop! system-atom)
  (start! system-atom))

(defonce system (atom {}))

(defn submit!
  [stuff]
  (if-let [in (-> @system :entrypoint)]
    (a/go
      (a/>! in stuff))
    (log/error "Accretion system is not running.")))


#_(-> @system :uniprot/protein-db-accrete :output-channel)
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
    (setup/accrete-databases! connection)
    (restart! system))

#_(start! system)

#_(-> @system :uniprot/protein-api :input-channel (.offer "P02919"))

;; (def sz 20)
;; (def c (a/chan sz))
;; (def mult-c (a/mult c))
;; (def cx (a/chan sz))
;; (def cy (a/chan sz))
;; (def cz (a/chan sz))
;; (a/tap mult-c cx)
;; (a/tap mult-c cy)
;; (a/tap mult-c cz)
;; (a/put! c "sent to all")
;; (a/<!! cx)
;; (a/<!! cy)
;; (a/<!! cz)

