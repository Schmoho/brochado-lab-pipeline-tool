(ns db
  (:refer-clojure :exclude [get])
  (:require
   [db.core :as db]
   [biodb.afdb :as afdb]
   [biodb.pubchem :as pubchem]
   [biodb.uniprot.api :as api.uniprot]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [csv-utils :as csv-utils]
   [data-cleaning :as clean]
   [utils :as utils]))

(defn- fs-pathify
  [path file file-type]
  (let [file-type (if (str/starts-with? file-type ".") (subs file-type 1) file-type)]
    (format "%s/%s.%s"
            (->> path (map name) (str/join "/"))
            file
            file-type)))

(defmulti insert! (fn [path file-type stuff & {:keys [read?]
                                               :or   {read? true}}] (type stuff)))

#_(defmethod insert! java.io.File
  [path file-type input-file & {:keys [read?]
                                :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/copy! (io/file fs-path) input-file)]
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

#_(defmethod insert! :default
  [path file-type stuff & {:keys [read?]
                           :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/write! (io/file fs-path) stuff)
        path          (map name path)]
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defn- acquire-structures-by-id!
  [protein-id]
  (let [structure (afdb/get-structure-files protein-id)]
    (if (or (< 1 (count (:pdb structure)))
            (< 1 (count (:cif structure))))
      (throw (ex-info "Multiple structure files. Deal with it!"
                      {:protein-id protein-id}))
      (let [{:keys [pdb cif]}
            (->  structure
                 (update :pdb first)
                 (update :cif first))]
        {:pdb (db/insert! [:raw :afdb :pdb protein-id] "pdb" pdb)
         :cif (db/insert! [:raw :afdb :cif protein-id] "cif" cif)}))))


(defn- get-edn-record
  [path id]
  (let [filename (fs-pathify path id "edn")]
    (try
      (utils/read-file filename)
      (catch Exception e
        (log/info "Tried to access non-existing file:"
                  (ex-message e))))))

(defmulti get-record (fn [path _id] path))
(defmethod get-record [:data :raw :uniprot :proteome]
  [path id]
  (get-edn-record path id))

(defmethod get-record [:data :raw :uniprot :uniprotkb]
  [path id]
  (with-meta
    (get-edn-record path id)
    api.uniprot/uniprotkb-entry-meta))

(defmethod get-record [:data :raw :afdb :pdb]
  [path id]
  (if-let [pdb-file (db/db-get [:raw :afdb :pdb id])]
    pdb-file
    (:pdb (acquire-structures-by-id! id))))

(defmethod get-record [:data :processed :obabel :ligand :pdbqt]
  [{:keys [id parameter-hash]}]
  (db/db-get (conj [:processed :obabel :ligand :pdbqt id parameter-hash]
                   id)
             :read? false))

(defmethod get-record [:data :processed :obabel :protein :pdbqt]
  [{:keys [protein-id params-hash]}]
  (db/db-get [:processed :obabel :protein :pdbqt protein-id params-hash]))

(defn- get-all-by-path
  [path xform]
  (->> path
       (map name)
       (str/join "/")
       (io/file)
       (utils/ffile-seq)
       (transduce
        xform
        (completing #(assoc %1 (first %2) (second %2)))
        {})))

(defmulti get-all-records (fn [path] path))

(defmethod get-all-records [:data :raw :uniprot :taxonomy]
  [path]
  (get-all-by-path
   [:data :raw :uniprot :taxonomy]
   (map
    (comp
     (juxt #(str (:taxonId %)) identity)
     utils/read-file))))

(defmethod get-all-records [:data :raw :uniprot :proteome]
  [path]
  (get-all-by-path
   path
   (map
    (comp
     (juxt utils/base-name
           utils/read-file)))))

(defmethod get-all-records [:data :raw :pubchem :compound]
  [path]
  (get-all-by-path
   path
   (map
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
     utils/read-file))))

(defmethod get-all-records [:data :input :volcano]
  [path]
  (->> path
       (map name)
       (str/join "/")
       (io/file)
       (utils/ffile-seq)
       (group-by #(.getName (.getParentFile %)))
       (map (fn [[id files]]
              [id (->> files
                       (map
                        (fn [file]
                          (let [type (keyword (utils/base-name file))]
                            [type
                             (cond
                               (= type :meta)  (utils/read-file  file)
                               (= type :table) (->>  file
                                                     csv-utils/read-csv-data
                                                     (map data-cleaning/numerify)))])))
                       (into {}))]))))



