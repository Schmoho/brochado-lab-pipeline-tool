(ns server.handler
  (:require [graph.accrete.core :as accrete]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [csv-utils :as csv-utils]
            [fast-edn.core :as edn]
            [clojure.string :as str]
            [pipeline.taxonomy :as pipeline.taxonomy]
            [utils :as utils])
  (:import (java.time Instant)))

(defn basic-id-handler
  [type id-accessor]
  (fn 
    [request]
    (let [id          (or (-> request :parameters :query id-accessor)
                          (-> request :parameters :body id-accessor))
          submission  {:id                       id
                       :requested-accretion-type type}
          expectation (promise)]
      (accrete/register-expectation! type id expectation)
      (log/debug "Submitting" submission)
      (accrete/submit! submission)
      {:status 200
       :body   (select-keys
                (deref expectation 10000
                       {:message "Timeout while waiting for confirmation."
                        :type    :confirmation-timeout})
                [:type :id])})))

(def uniprot-taxon-id-handler (basic-id-handler :uniprot/taxon :taxon-id))
(def uniprot-protein-id-handler (basic-id-handler :uniprot/protein :protein-id))

(def form
  {:params.uniprot/taxonomy {:use-taxonomic-search? true},
   :params.uniprot/uniref
   {:use-uniref? true, :cluster-types #{:uniref-100 :uniref-90}},
   :params.uniprot/blast {:use-blast? false},
   :params.uniprot/protein
   {:protein-ids
    ["A0A0H2ZHP9" "A0A0H2ZHP9" "A0A0H2ZHP9" "A0A0H2ZHP9" "A0A0H2ZHP9"],
    :gene-names ["mrcB"]}})

(defn start-msa-handler
  [request]
  (tap> request)
  (let [uuid (random-uuid)
        form (-> request :body-params (assoc :pipeline/uuid uuid))]
    (log/info "Run job with UUID" uuid)
    (log/info "Write params.")
    (utils/write!
     (format "data/results/msa/%s/params.edn" (str uuid)))
    (future (try
              (pipeline.taxonomy/pipeline form)
              (catch Throwable t
                (log/error t))))
    {:status 200
     :body   {:job-id uuid}}))


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
   :body   {:taxon (->> (file-seq (io/file "data/raw/uniprot/taxonomy"))
                        (filter #(.isFile %))
                        (mapv (comp
                               (juxt :id identity)
                               #(assoc % :id (str (:taxonId %)))
                               utils/read-file))
                        (into {}))
            :proteome (->> (file-seq (io/file "data/raw/uniprot/proteome"))
                           (filter #(.isFile %))
                           (mapv (comp
                                  (juxt (comp
                                         #(str/replace % ".edn" "")
                                         #(.getName %))
                                        utils/read-file)))
                           (into {}))}})


(defn get-ligands
  [request]
  (tap> request)
  (log/info "Getting ligand data.")
  {:status 200
   :body   {:ligand (->> (file-seq (io/file "data/raw/pubchem/compound"))
                         (filter #(.isFile %))
                         (mapv
                          (comp
                           (juxt :id identity)
                           (fn [data]
                             (-> (assoc data :id (-> data :json :PC_Compounds first :id :id :cid str))
                                 (assoc :json (get-in data [:json :PC_Compounds]))
                                 (update :json
                                         (comp #(dissoc % :bonds)
                                               #(dissoc % :atoms)
                                               #(dissoc % :stereo)
                                               #(dissoc % :coords)
                                               first))
                                 (dissoc :sdf)))
                           utils/read-file))
                         (into {}))}})

;; (defn get-volcanos
;;   [request]
;;   (tap> request)
;;   (log/info "Getting volcano data.")
;;   {:status 200
;;    :body   {:ligand (->> (file-seq (io/file "data/input/volcano"))
;;                          (filter #(.isFile %)))}})

(defn get-volcanos
  [request]
  (let [volcano-dir (io/file "data/input/volcano/")
        data (->> (file-seq volcano-dir)
                  (filter #(and (not= % volcano-dir)
                                (.isFile %)))
                  (mapv
                   (fn [file]
                     (let [id (.getName (.getParentFile file))
                           file-extension (utils/extension (.getName file))
                            ;; I am expecting schematic names here like "table.csv"
                            ;; and "meta.edn"
                           file-content-key (keyword (str/replace (.getName file)
                                                                  (str "." file-extension)
                                                                  ""))]
                       {id {file-content-key (if (= "csv" file-extension)
                                               (csv-utils/read-csv-data file)
                                               (utils/read-file file))}})))
                  (apply merge-with merge))]
    (tap> data)
    {:status 200
     :body   {:volcano (or data [])}}))


(defn upload-volcano
  [request]
  (tap> request)
  (log/info "Writing volcano.")
  (let [id          (random-uuid)
        upload-form (-> request
                        :body-params
                        (update :meta assoc :id id)
                        #_(update :meta assoc :timestamp (Instant/now)))]
    (utils/write! (format "data/input/volcano/%s/meta.edn" id)
                  (-> upload-form :meta))
    (csv-utils/write-csv-data! (format "data/input/volcano/%s/table.csv" id)
                               (-> upload-form :file))
    {:status 200
     :body   {id (:meta upload-form)}}))
