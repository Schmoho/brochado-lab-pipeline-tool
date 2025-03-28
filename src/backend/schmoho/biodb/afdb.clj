(ns schmoho.biodb.afdb
  (:require
   [schmoho.biodb.http :as http]))

(def alpha-fold-db-api-base "https://alphafold.ebi.ac.uk/api")

;; --- Simple Wrappers ---

(def info-meta
  {:biodb/source :afdb
   :afdb/type :info})

(def alpha-fold-info
  (http/id-query
   (str (format "%s/prediction/" alpha-fold-db-api-base) "%s")
   info-meta))

#_(alpha-fold-info "A0A0H2ZHP9")

;; --- Convenience Wrapper ---

(defn get-pdb
  [id]
  (let [info (first (alpha-fold-info id))]
    {:structure (-> info :pdbUrl http/get :body)
     :meta      {:name               "AlphaFold Structure"
                 :protein            id
                 :source             :afdb
                 :model-created-date (:modelCreatedDate info)
                 :model-version      (:latestVersion info)}}))

#_(get-pdb "A0A0H2ZHP9")

(defn get-cif
  [id]
  (let [info (alpha-fold-info id)]
    (map (comp :body http/get :cifUrl) info)))

#_(get-cif "A0A0H2ZHP9")

(defn get-structure-files
  [id]
  (let [info (alpha-fold-info id)]
    {:cif (map (comp :body http/get :cifUrl) info)
     :pdb (map (comp :body http/get :pdbUrl) info)}))

#_(get-structure-files "A0A0H2ZHP9")
