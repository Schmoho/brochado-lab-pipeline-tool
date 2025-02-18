(ns biodb.afdb
  (:require [clj-http.client :as client]
            [utils :refer [get-json]]))


(defn get-alpha-fold-db-record!
  [id]
  (let [info (-> (get-json (str "https://alphafold.ebi.ac.uk/api/prediction/" id))
                 first)
        pdb  (-> (client/get (:pdbUrl info))
                :body)]
    [id {:info info
         :pdb  pdb}]))

(defn afdb-id-from-uniprot-record
  [uniprot-record]
  (->> uniprot-record
       :uniProtKBCrossReferences
       (filter #(= "AlphaFoldDB"
                   (:database %)))
       first
       :id))

(def get-structures-for-uniprot-records!
  (memoize
   (fn [records]
     (->> records
          (map (fn [[id record]]
                 (when-let [afdb-id (afdb-id-from-uniprot-record record)]
                   (get-alpha-fold-db-record! afdb-id))))
          (filter some?)
          (into {})))))
