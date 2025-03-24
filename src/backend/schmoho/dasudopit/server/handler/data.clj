(ns schmoho.dasudopit.server.handler.data
  (:require
   [schmoho.biodb.afdb :as afdb]
   [schmoho.dasudopit.db :as db]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [schmoho.biodb.pubchem :as pubchem]
   [schmoho.biodb.uniprot.api :as uniprot.api]
   [schmoho.biotools.obabel :as obabel]
   [schmoho.utils.file :as utils]))

(defn get-metadata
  [request]
  (tap> request)
  (try
    (let [path    (str/replace (:uri request) "/api" "")
          dataset (db/get-metadata path)]
      (tap> path)
      (tap> dataset)
      (if dataset
        {:status 200
         :body   dataset}
        {:status 404
         :body   {:message "Unknown dataset."}}))
    (catch Exception e
      (log/error e)
      (throw e))))

(defn get-dataset
  ([request]
   (tap> request)
   (let [path    (str/replace (:uri request) "/api" "")
         dataset (db/get-dataset path)]
     (if dataset
       {:status 200
        :body   dataset}
       {:status 404
        :body   {:message "Unknown dataset."}})))
  ([provisioning-fn request]
   (tap> request)
   (let [path    (str/replace (:uri request) "/api" "")
         dataset (db/get-dataset path)]
     (if dataset
       {:status 200
        :body   dataset}
       {:status 200
        :body   (provisioning-fn (:path-params request)
                               path)}))))

(defn delete-dataset!
  [request]
  (tap> request)
  (let [path (str/replace (:uri request) "/api" "")]
    (db/delete-dataset! path)
    {:status 200
     :body   {:message "Dataset deleted."}}))

(defn update-metadata!
  [request]
  (tap> request)
  (let [path         (str/replace (:uri request) "/api" "")
        new-meta     (-> request :body-params)
        updated-meta (db/update-metadata! path new-meta)]
    {:status 200
     :body   updated-meta}))

(defn upload-dataset!
  [request]
  (let [path    (str/replace (:uri request) "/api" "")
        dataset (-> request :body-params)]
    (db/upload-dataset! path dataset)
    {:status 200
     :body   {:message "Dataset created."}}))

(defn provision-afdb-structure
  [{:keys [protein-id]} path]
  (let [data (afdb/get-pdb protein-id)]
    (db/upload-dataset! path data)))

(defn search-ligand
  [request]
  (tap> request)
  ;; the param is named ID, but we'll also support search by name
  (let [search-thing (-> request :path-params :id)
        results      (try
                       [(str search-thing) (pubchem/get-compound-data-by-id search-thing :sdf? false)]
                       (catch Exception _
                         (try
                           (pubchem/search-compound-by-name search-thing)
                           (catch Exception _
                             (log/info "Did not find compound via" search-thing)
                             []))))]
    {:status 200
     :body   results}))

(defn provision-ligand
  [request]
  (let [path       (str/replace (:uri request) "/api" "")
        pubchem-id (-> request :path-params :id)
        results    (pubchem/get-compound-data-by-id pubchem-id)]
    (db/upload-dataset! path results)
    (when (utils/is-command-available? "obabel")
      (log/info "Producing 3D conformer for" pubchem-id)
      (let [obabel-meta {:params  obabel/obabel-3d-conformer-args
                         :version (obabel/obabel-version)}]
        (db/upload-dataset! (str path "/processed")
                            {:docking-ready (obabel/produce-obabel-3d-conformer! (:sdf results))
                             :meta          obabel-meta})))
    {:status 200
     :body   results}))

#_(provision-ligand {:path-params {:id "37768"}
                     :uri "/api/data/ligand/37768"})

(defn search-taxon
  [request]
  (tap> request)
  (let [taxon-id (-> request :path-params :id)
        taxon    (uniprot.api/taxonomy-entry taxon-id {:fields uniprot.api/taxon-fields-of-interest})
        proteome (-> (if (-> taxon :statistics :referenceProteomeCount pos-int?)
                       (uniprot.api/ref-proteomes-by-taxon-id taxon-id)
                       (uniprot.api/proteomes-by-taxon-id taxon-id))
                     first)]
    {:status 200
     :body   {:taxon    (assoc taxon :id (:taxonId taxon))
              :proteome proteome}}))

#_(search-taxon {:path-params {:id "208964"}})

(defn provision-taxon
  [request]
  (let [path          (str/replace (:uri request) "/api" "")
        taxon-id      (-> request :path-params :id)
        taxon         (uniprot.api/taxonomy-entry taxon-id {:fields "id,common_name,scientific_name,lineage,statistics"})
        proteome      (-> (if (-> taxon :statistics :referenceProteomeCount pos-int?)
                            (uniprot.api/ref-proteomes-by-taxon-id taxon-id)
                            (uniprot.api/proteomes-by-taxon-id taxon-id))
                          first)
        taxon-dataset {:data (assoc taxon :id (str (:taxonId taxon)))
                       :meta {:id   (str (:taxonId taxon))
                              :name (:scientificName taxon)}}]
    (db/upload-dataset! path taxon-dataset)
    (future (let [proteins (uniprot.api/proteins-by-proteome (:id proteome))]
              (db/upload-dataset! (str path "/proteome")
                                  {:data (map #(assoc % :id (:primaryAccession %)) proteins)
                                   :meta {:proteome-id   (:id proteome)
                                          :taxon-id      taxon-id
                                          :proteome-type (:proteomeType proteome)}})
              (log/info "Added proteome" (:id proteome) "for" taxon-id)))
    {:status 200
     :body   taxon-dataset}))

#_(provision-taxon {:path-params {:id "83333"}
                    :uri "/api/data/taxon/83333"})
