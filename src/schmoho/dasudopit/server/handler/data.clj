(ns schmoho.dasudopit.server.handler.data
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [fast-edn.core :as edn]
   [schmoho.dasudopit.biodb.afdb :as afdb]
   [schmoho.dasudopit.db :as db]
   [schmoho.dasudopit.pipeline.taxonomy :as pipeline.taxonomy]))

;; TODO: das ganze Zeug hier sollte über DB API laufen!
(defn get-msa-results-handler
  [request]
  (log/info "Getting taxonomic comparison results.")
  (let [results (->> (file-seq (io/file "data/results/msa"))
                     (filter #(and (not= % (io/file "data/results/msa"))
                                   (.isFile %)
                                   (str/ends-with? (.getName %) ".edn")))
                     (mapv #(edn/read-once (io/file %)))
                     (mapv (fn [result]
                             (if (@pipeline.taxonomy/running-blast-jobs
                                  (:pipeline/uuid result))
                               (assoc result :blast-still-running? true)
                               result))))]
    {:status 200
     :body   {:results results}}))

(defn get-taxons
  [request]
  (tap> request)
  (log/info "Getting taxon data.")
  {:status 200
   :body   (db/get-all-records [:data :raw :uniprot :taxonomy])})

(defn get-taxon
  [request]
  (tap> request)
  (let  [id (-> request :path-params :id)]
    (log/info "Getting taxon data for ID" id)
    {:status 200
     :body   (db/get-record [:data :raw :uniprot :taxonomy] id)}))

(defn get-proteome
  [request]
  (tap> request)
  (let [id (-> request :path-params :id)]
    (log/info "Getting proteome data for ID" id)
    {:status 200
     :body   (db/get-record [:data :raw :uniprot :proteome] id)}))

(defn get-ligands
  [request]
  (tap> request)
  (log/info "Getting ligand data.")
  {:status 200
   :body   (db/get-all-records [:data :raw :pubchem :compound])})

(defn get-ligand
  [request]
  (tap> request)
  (let [id (-> request :path-params :id)]
    (log/info "Getting ligand data.")
    {:status 200
     :body   (db/get-record [:data :raw :pubchem :compound] id)}))

#_"https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/37768/xrefs/RegistryID,RN,SBURL/JSON"

(defn get-volcanos
  [request]
  (tap> request)
  {:status 200
   :body   (db/get-all-records [:data :input :volcano])})

(defn get-afdb-structure
  [request]
  (tap> request)
  (let [id               (-> request :path-params :id)
        save-when-fetch? (-> request :params :save-when-fetch?)]
    (log/debug "Get structure for " id)
    (if-let [db-record (db/get-record [:data :raw :afdb :pdb] id)]
      {:status 200
       :body   db-record}
      (let [data (afdb/get-pdb id)]
        (when save-when-fetch?
          (db/insert! [:data :raw :afdb :pdb] id data))
        {:status 200
         :body   data}))))

(defn get-input-structure
  [request]
  (tap> request)
  (let [protein-id (-> request :path-params :protein-id)
        id (-> request :path-params :id)]
    (log/debug "Get structure" id "for" protein-id)
    (let [db-record (db/get-record [:data :input :structure] [protein-id id])]
      {:status 200
       :body   db-record})))

(defn get-processed-structure
  [request]
  (tap> request)
  (let [protein-id (-> request :path-params :protein-id)
        id (-> request :path-params :id)]
    (log/debug "Get structure" id "for" protein-id)
    (let [db-record (db/get-record [:data :processed :structure] [protein-id id])]
      {:status 200
       :body   db-record})))

(defn upload-volcano
  [request]
  (tap> request)
  (log/info "Writing volcano.")
  (let [id          (str (random-uuid))
        upload-form (-> request
                        :body-params
                        (update :meta assoc :id id)
                        #_(update :meta assoc :timestamp (Instant/now)))]
    (db/insert! [:data :input :volcano] id upload-form)
    {:status 200
     :body   {id upload-form}}))
