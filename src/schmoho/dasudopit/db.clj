(ns schmoho.dasudopit.db
  (:refer-clojure :exclude [get])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schmoho.dasudopit.biodb.uniprot.api :as api.uniprot]
   [schmoho.dasudopit.csv-utils :as csv-utils]
   [schmoho.dasudopit.data-cleaning :as clean]
   [schmoho.dasudopit.utils :as utils]))

(defn- fs-pathify
  ([path file-name file-type]
   (fs-pathify (concat path (if (coll? file-name)
                              file-name
                              [file-name])) file-type))
  ([path file-type]
   (let [file-type (if (str/starts-with? file-type ".") (subs file-type 1) file-type)]
     (str (->> path
               (map name)
               (str/join "/"))
          "."
          file-type)))
  ([path]
   (->> path
        (map name)
        (str/join "/"))))

(defn- index-folder
  [files]
  (->> files
       (map
        (fn [file]
          (let [type (keyword (utils/base-name file))]
            [type
             (cond
               (= type :table) (->>  file
                                     csv-utils/read-csv-data
                                     (map clean/numerify))
               :else  (utils/read-file file))])))
       (into {})))

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

(defn- insert-file
  [path file-type input-file read?]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/copy! (io/file fs-path) input-file)]
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defmulti insert!
  (fn [path _id _stuff] path))

(defmethod insert! [:data :raw :afdb :pdb]
  [path id {:keys [meta structure]}]
  (utils/write!
   (fs-pathify (concat path [id])
               "meta"
               "edn")
   meta)
  (csv-utils/write-csv-data!
   (fs-pathify (concat path [id])
               "structure"
               "pdb")
   structure))

(defmethod insert! [:data :input :volcano]
  [path id {:keys [meta table]}]
  (utils/write!
   (fs-pathify (concat path [id])
               "meta"
               "edn")
   meta)
  (csv-utils/write-csv-data!
   (fs-pathify (concat path [id])
               "table"
               "csv")
   table))

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

#_(->> (get-record [:data :raw :uniprot :proteome] "208963")
     (walk/postwalk
      (fn [e]
        (if (and (map? e)
                 (:evidenceCode e))
          (assoc e :evidenceDescription (eco/term-lookup (:evidenceCode e)))
          e))))

(defmethod get-record [:data :raw :uniprot :uniprotkb]
  [path id]
  (with-meta
    (get-edn-record path id)    
    api.uniprot/uniprotkb-entry-meta))

(defmethod get-record [:data :raw :afdb :pdb]
  [path id]
  (let [folder (fs-pathify (concat path [id]))]
    (->> folder
         utils/ffile-seq
         index-folder)))

(defmethod get-record [:data :input :structure]
  [path ids]
  (let [folder (fs-pathify (concat path ids))]
    (->> folder
         utils/ffile-seq
         index-folder)))

(defmethod get-record [:data :processed :structure]
  [path ids]
  (let [folder (fs-pathify (concat path ids))]
    (->> folder
         utils/ffile-seq
         index-folder)))

(defn get-all-structures-for-protein-id
  [protein-id]
  {:afdb      (get-record [:data :raw :afdb :pdb])
   :input     (->> nil
                   (utils/folder-seq)
                   (map (fn [folder]
                          [(.getName folder)
                           (->> folder
                                utils/ffile-seq
                                index-folder)]))
                   (into {}))
   :processed (->> nil
                   (utils/folder-seq)
                   (map (fn [folder]
                          [(.getName folder)
                           (->> folder
                                utils/ffile-seq
                                index-folder)]))
                   (into {}))})

#_(get-record [:data :raw :afdb :pdb] "A0A0H2ZHP9")

(defmethod get-record [:data :raw :uniprot :taxonomy]
  [path id]
  (with-meta
    (get-edn-record path (str id))
    api.uniprot/taxon-meta))

(defmethod get-record [:data :raw :pubchem :compound]
  [path id]
  (let [data (get-edn-record path (str id))]
    (-> (assoc data :id (-> data :json :PC_Compounds first :id :id :cid str))
        (assoc :json (get-in data [:json :PC_Compounds]))
        (update :json
                (comp #(dissoc % :bonds)
                      #(dissoc % :atoms)
                      #(dissoc % :stereo)
                      #(dissoc % :coords)
                      first))
        (dissoc :sdf))))

(defmethod get-record [:data :processed :obabel :ligand :pdbqt]
  [path {:keys [id params-hash]}]
  (->> (concat path [id params-hash])
       fs-pathify
       utils/ffile-seq
       index-folder))

(defmethod get-record [:data :processed :obabel :protein :pdbqt]
  [path {:keys [id params-hash]}]
  (->> (concat path [id params-hash])
       fs-pathify
       utils/ffile-seq
       index-folder))

(defmulti get-all-records (fn [path] path))

(defmethod get-all-records [:data :raw :uniprot :taxonomy]
  [path]
  (get-all-by-path
   path
   (map
    (comp
     (juxt #(str (:taxonId %)) #(assoc % :id (str (:taxonId %))))
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
              [id (index-folder files)]))
       (into {})))




;; (defn- acquire-structures-by-id!
;;   [protein-id]
;;   (let [structure (afdb/get-structure-files protein-id)]
;;     (if (or (< 1 (count (:pdb structure)))
;;             (< 1 (count (:cif structure))))
;;       (throw (ex-info "Multiple structure files. Deal with it!"
;;                       {:protein-id protein-id}))
;;       (let [{:keys [pdb cif]}
;;             (->  structure
;;                  (update :pdb first)
;;                  (update :cif first))]
;;         {:pdb (db/insert! [:raw :afdb :pdb protein-id] "pdb" pdb)
;;          :cif (db/insert! [:raw :afdb :cif protein-id] "cif" cif)}))))
