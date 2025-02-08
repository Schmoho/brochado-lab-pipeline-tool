(ns mongo.uniprot
  (:require [monger.core :as mg]
            [monger.db]
            [monger.collection :as mc]
            [clojure.tools.logging :as log])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]))


(defn save-uniprot-proteome!
  [proteome-id proteins]
  (log/debug "Save" (count proteins) "proteins for proteome" proteome-id "in MongoDB.")
  (let [conn   (mg/connect)
        db     (mg/get-db conn "api-cache")
        batch  (mc/insert-and-return
                db
                "batch-requests"
                {:_id         (ObjectId.)
                 :proteome-id proteome-id})
        result (when (not-empty proteins) (mc/insert-batch db "proteins" proteins))]
  (mg/disconnect conn)
  batch))

(defn get-uniprot-proteome!
  [proteome-id]
  (let [conn           (mg/connect)
        db             (mg/get-db conn "api-cache")
        previous-batch (mc/find-one-as-map db "batch-requests" {:proteome-id proteome-id})]
    (if previous-batch
      (let [_      (log/debug "Found previous batch" previous-batch "in MongoDB")
            result (doall
                    (mc/find-maps db "proteins"
                                  {"uniProtKBCrossReferences.database" "Proteomes"
                                   "uniProtKBCrossReferences.id"       proteome-id}))]
        (mg/disconnect conn)
        result)
      (do
        (log/debug "Did not find previous batch in MongoDB.")
        (mg/disconnect conn)
        nil))))
