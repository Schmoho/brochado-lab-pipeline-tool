(ns schmoho.dasudopit.server.handler.data
  (:require
   [schmoho.dasudopit.biodb.afdb :as afdb]
   [schmoho.dasudopit.db :as db]
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [schmoho.dasudopit.biodb.pubchem :as pubchem]))

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
                       [search-thing (pubchem/get-compound-data-by-id search-thing :sdf? false)]
                       (catch Exception _
                         (try
                           (pubchem/search-compound-by-name search-thing)
                           (catch Exception _
                             (log/info "Did not find compound via" search-thing)
                             []))))]
    (tap> results)
    {:status 200
     :body   results}))
