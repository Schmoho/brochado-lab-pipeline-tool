(ns schmoho.dasudopit.server.handler.data
  (:require
   [schmoho.biodb.afdb :as afdb]
   [schmoho.dasudopit.db :as db]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [schmoho.biodb.pubchem :as pubchem]
   [schmoho.biodb.uniprot.api :as uniprot.api]
   [schmoho.biotools.obabel :as obabel]
   [schmoho.utils.file :as utils]
   [clojure.java.io :as io]
   [babashka.fs :as fs]))

(defn get-metadata
  [request]
  (tap> request)
  (try
    (let [path    (str/replace (:uri request) "/api/" "")
          dataset (db/get-metadata path)]
      (if dataset
        {:status 200
         :body   dataset}
        {:status 404
         :body   {:message "Unknown dataset."}}))
    (catch Exception e
      (log/error e)
      (throw e))))

(def request {:uri "/api/data/structure/P02919"})

(defn get-structures-metadata
  [request]
  (tap> request)
  (try
    (let [path    (str/replace (:uri request) "/api/" "")
          dataset (->> (utils/ffile-seq path)
                       (filter #(and (= "meta" (utils/base-name %))
                                     (or (str/includes? (.getPath %) "input")
                                         (str/includes? (.getPath %) "processed"))))
                       (map (juxt (comp
                                   #(concat % [:meta])
                                   #(str/split % #"/")
                                   #(str/replace % (str path "/") "")
                                   #(.getParent %))
                                  utils/read-file))
                       (reduce
                        (fn [acc [path meta]]
                          (assoc-in acc path meta))
                        {}))]
      (tap> dataset)
      {:status 200
       :body   dataset})
    (catch Exception e
      (log/error e)
      (throw e))))

(defn get-single-structures-metadata
  [request]
  (tap> request)
  (try
    (let [protein-id (-> request :path-params :protein-id)
          path       (str/replace (:uri request) "/api/" "")
          dataset    (->> (utils/ffile-seq path)
                       (filter #(and (str/includes? (fs/file-name %) "meta")
                                     (or (str/includes? (.getPath %) "input")
                                         (str/includes? (.getPath %) "processed"))))
                       (map (juxt (comp
                                   #(concat % [:meta])
                                   (partial drop-while (into #{} (str/split path #"/")))
                                   (partial map fs/file-name)
                                   fs/components
                                   fs/parent)
                                  utils/read-file))
                       (reduce
                        (fn [acc [path meta]]
                          (assoc-in acc path meta))
                        {}))
          dataset    (if-not (get dataset "afdb")
                       (let [afdb-meta (:meta (afdb/get-pdb protein-id))]
                         (assoc dataset "afdb" {:meta afdb-meta}))
                       dataset)]
      (tap> dataset)
      {:status 200
       :body   dataset})
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
         dataset (not-empty (db/get-dataset path))]
     (if dataset
       {:status 200
        :body   dataset}
       (let [provisioning-result (provisioning-fn (:path-params request) path)]
         {:status 200
          :body   provisioning-result})))))


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
        id         (str (random-uuid))
        dataset    (-> request
                       :body-params
                       (assoc-in [:meta :id] id))]
    (try
      (db/upload-dataset! (str path "/" id) dataset)
      (catch Exception e
        (prn e)
        (throw e)))
    {:status 200
     :body   {id dataset}}))

(defn provision-afdb-structure
  [{:keys [protein-id]} path]
  (let [data (afdb/get-pdb protein-id)]
    (db/upload-dataset! path data)
    data))

(defn search-ligand
  [request]
  (tap> request)
  ;; the param is named ID, but we'll also support search by name
  (let [search-thing (-> request :path-params :id)
        results      (try
                       (pubchem/get-compound-data-by-id search-thing :sdf? false)
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

(defn proteome-for-taxon
  [taxon]
  (let [taxon-id (:taxonId taxon)
        proteome (-> (if (-> taxon :statistics :referenceProteomeCount pos-int?)
                       (uniprot.api/ref-proteomes-by-taxon-id taxon-id)
                       (uniprot.api/proteomes-by-taxon-id taxon-id))
                     first)]
    (if (= "Redundant proteome" (:proteomeType proteome))
      (with-meta (uniprot.api/proteomes-entry (:redundantTo proteome))
        {:instead-of-redundant (:id proteome)})
      proteome)))

(defn search-taxon
  [request]
  (tap> request)
  (let [taxon-id (-> request :path-params :id)
        taxon    (uniprot.api/taxonomy-entry taxon-id {:fields uniprot.api/taxon-fields-of-interest})
        proteome (proteome-for-taxon taxon)]
    {:status 200
     :body   {:taxon    (assoc taxon :id (:taxonId taxon))
              :proteome (merge (select-keys proteome
                                            [:id :proteomeType :proteinCount])
                               (meta proteome))}}))

#_(search-taxon {:path-params {:id "208964"}})

(defn provision-taxon
  [request]
  (let [path          (str/replace (:uri request) "/api" "")
        taxon-id      (-> request :path-params :id)
        taxon         (uniprot.api/taxonomy-entry taxon-id {:fields "id,common_name,scientific_name,lineage,statistics"})
        proteome      (proteome-for-taxon taxon)
        taxon-dataset {:data (assoc taxon :id (str (:taxonId taxon)))
                       :meta {:id   (str (:taxonId taxon))
                              :name (:scientificName taxon)}}]
    (db/upload-dataset! path taxon-dataset)
    (future (let [proteins (uniprot.api/proteins-by-proteome (:id proteome))]
              (db/upload-dataset! (str path "/proteome")
                                  {:data (let [proteome (map #(assoc % :id (:primaryAccession %)) proteins)]
                                           (zipmap
                                            (map :id proteome)
                                            proteome))
                                   :meta (merge {:proteome-id   (:id proteome)
                                                 :taxon-id      taxon-id
                                                 :proteome-type (:proteomeType proteome)}
                                                (meta proteome))})
              (log/info "Added proteome" (:id proteome) "for" taxon-id)))
    {:status 200
     :body   taxon-dataset}))

#_(provision-taxon {:path-params {:id "83333"}
                    :uri "/api/data/taxon/83333"})

(defn save-structure
  [request]
  (let [path       (str/replace (:uri request) "/api" "")
        protein-id (-> request :path-params :protein-id)
        id         (str (random-uuid))
        dataset    (-> request
                       :body-params
                       (assoc-in [:meta :id] id))]
    (try
      (db/upload-dataset! (str path "/" id) dataset)
      (catch Exception e
        (prn e)
        (throw e)))
    {:status 200
     :body   {id dataset}}))
