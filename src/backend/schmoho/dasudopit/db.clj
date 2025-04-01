(ns schmoho.dasudopit.db
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schmoho.utils.csv :as csv-utils]
   [schmoho.utils.data-cleaning :as clean]
   [schmoho.utils.file :as utils]))

(defn- pathify
  [file]
  (let [segments (-> file (.getPath) (str/split #"/"))]
    (conj (mapv keyword (butlast segments))
          (keyword (utils/base-name (last segments))))))

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

(defn- drop-common-vector-prefix
  "Already assumes the vectors have common prefix."
  [v1 v2]
  (let [v1-count  (count v1)
        v2-count  (count v2)
        i (min v1-count v2-count)]
    (vec (drop i (if (< v1-count v2-count)
                   v2
                   v1)))))

(defn get-metadata
  [path]
  (let [path (if (str/starts-with? path "/")
               (subs path 1)
               path)
        prefix-segments (map keyword (str/split path #"/"))]
    (->> path
         utils/ffile-seq
         (filter #(= "meta" (utils/base-name %)))
         (map (juxt pathify utils/read-file))
         (reduce
          (fn [acc [path data]]
            (assoc-in acc (update (drop-common-vector-prefix
                                   path
                                   prefix-segments)
                                  0
                                  name)
                      data))
          {}))))

#_(get-metadata "/data/volcano")


(defn get-dataset
  [path]
  (let [path (if (str/starts-with? path "/")
               (subs path 1)
               path)]
    (->> path
         utils/ffile-seq
         index-folder)))

#_(get-dataset "/data/ligand/2244")

(defn delete-dataset!
  [path]
  (try
    (let [path    (if (str/starts-with? path "/")
                  (subs path 1)
                  path)
          _       (when-not (str/starts-with? path "data")
                  (throw (ex-info "Probably not deleting what you want."
                                  {:path path})))
          files   (distinct (reverse (utils/ffile-seq (io/file path))))]
      (doseq [f files]
        (log/info "Delete file" (.getPath f))
        (io/delete-file f))
      (let [folders (distinct (reverse (file-seq (io/file path))))]
        (doseq [f folders]
         (log/info "Delete folder" (.getPath f))
         (io/delete-file f))))
    (catch Exception e
      (log/error e)
      (throw e))))

#_(delete-dataset! "/data/volcano/5c71b78c-e60f-412b-b61b-049fdc420c8d")

(defn update-metadata!
  [path meta]
  (let [path      (if (str/starts-with? path "/")
                    (subs path 1)
                    path)
        meta-path (str path "/meta.edn")
        old-meta  (utils/read-file meta-path)]
    (utils/write! meta-path (merge old-meta meta))
    (merge old-meta meta)))

#_(update-dataset! "/data/volcano/f5550dc9-2c45-46a5-b486-6aec1d9eac22"
                   {:name "Updated name"})

(defn upload-dataset!
  [path data]
  (let [path (if (str/starts-with? path "/")
               (subs path 1)
               path)]
    (doseq [[filename-kw data] data]
      (cond
        (= :table filename-kw)
        (let [filename "table.csv"]
          (csv-utils/write-csv-data! (str path "/" filename)
                                     data))
        :else
        (let [filename (case filename-kw
                         :meta          "meta.edn"
                         :structure     "structure.pdb"
                         :docking-ready "structure.pdbqt"
                         :data          "data.edn"
                         :image         "image.b64"
                         :sdf           "structure.sdf")]
          (utils/write! (str path "/" filename)
                        data))))))

#_(->> (get-record [:data :raw :uniprot :proteome] "208963")
     (walk/postwalk
      (fn [e]
        (if (and (map? e)
                 (:evidenceCode e))
          (assoc e :evidenceDescription (eco/term-lookup (:evidenceCode e)))
          e))))
