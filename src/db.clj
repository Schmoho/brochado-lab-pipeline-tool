(ns db
  (:require
   [biodb.afdb :as afdb]
   [biodb.pubchem :as pubchem]
   [biodb.uniprot.api :as api.uniprot]
   [biotools.obabel :as obabel]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [utils :as utils]))

(def db (atom {}))

(defn- fs-pathify
  [path file-type]
  (let [file-type (if (str/starts-with? file-type ".") (subs file-type 1) file-type)]
    (str (->> path (map name) (str/join "/"))
         "."
         file-type)))

(defmulti insert! (fn [path file-type stuff] (type stuff)))

(defmethod insert! java.io.File
  [path file-type input-file]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/copy! (io/file fs-path) input-file)]
    (swap! db #(assoc-in % path fs-path))
    inserted-file))

(defmethod insert! :default
  [path file-type stuff]
  (let [fs-path (fs-pathify path file-type)]
    (utils/write! (io/file fs-path) stuff)
    (swap! db #(assoc-in % path fs-path))
    (io/file fs-path)))

(defn get
  [path]
  (utils/read (get-in @db path)))

(defn initialize-db
  []
  (reset! db {}))

(defn protein-by-id
  [protein-id]
  (if-let [protein (get [:raw :uniprot :uniprotkb protein-id])]
    protein
    (let [protein (api.uniprot/uniprotkb-entry protein-id)]
      (insert! [:raw :uniprot :uniprotkb protein-id] "edn" protein))))

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
        {:pdb (insert! [:raw :afdb :pdb protein-id] "pdb" pdb)
         :cif (insert! [:raw :afdb :cif protein-id] "cif" cif)}))))

(defn pdb-by-id
  [protein-id]
  (if-let [pdb-file (get [:raw :afdb :pdb protein-id])]
    pdb-file
    (:pdb (acquire-structures-by-id! protein-id))))

(defn- acquire-compound-sdf-by-id!
  [pubchem-compound-id]
  (let [sdf (pubchem/get-sdf-by-compound-id pubchem-compound-id)]
    (insert! [:raw :pubchem :compound pubchem-compound-id] sdf)))

(defn sdf-by-compound-id
  [pubchem-compound-id]
  (if-let [sdf-file (get [:raw :pubchem :compound pubchem-compound-id])]
    sdf-file
    (acquire-compound-sdf-by-id! pubchem-compound-id)))

(defn pdbqt-file-by-compound-id
  [pubchem-compound-id]
  (let [obabel-hash (hash {:args    obabel/obabel-3d-conformer-args
                           :version (obabel/obabel-version)})
        path [:processed :obabel :pdbqt pubchem-compound-id obabel-hash]]
    (if-let [pdbqt-file (get path)]
      pdbqt-file
      (let [sdf-file        (sdf-by-compound-id pubchem-compound-id)
            output-tmp-file (utils/create-temp-file "pdbqt")]
        (obabel/produce-obabel-3d-conformer! sdf-file
                                             output-tmp-file
                                             obabel/obabel-3d-conformer-args)
        (insert! path
                 "pdbqt"
                 output-tmp-file)))))
