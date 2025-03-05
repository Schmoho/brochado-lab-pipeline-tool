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

(defn initialize-db!
  [data-folder]
  (let [tree (->> (file-seq (io/file data-folder))
                  (filter #(.isFile %))
                  (map
                   (comp
                    (fn [path]
                      (let [file-name  (last path)
                            path       (drop 1 (butlast path))
                            dot-ending (str "." (utils/extension file-name))
                            id         (if (str/ends-with? file-name dot-ending)
                                         (subs file-name 0 (- (count file-name)
                                                              (count dot-ending)))
                                         file-name)]
                        [(concat path [id]) file-name]))
                    #(str/split % #"/")
                    #(.getPath %)))
                  (reduce (fn [acc [path file-name]]
                            (assoc-in acc path file-name))
                          {}))]
    (reset! db tree)))

#_(initialize-db! "data")

(defn- fs-pathify
  [path file-type]
  (let [file-type (if (str/starts-with? file-type ".") (subs file-type 1) file-type)]
    (str "data/"
         (->> path (map name) (str/join "/"))
         "."
         file-type)))

(defmulti insert! (fn [path file-type stuff & {:keys [read?]
                                               :or   {read? true}}] (type stuff)))

(defmethod insert! java.io.File
  [path file-type input-file & {:keys [read?]
                                :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/copy! (io/file fs-path) input-file)]
    (swap! db #(assoc-in % path fs-path))
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defmethod insert! :default
  [path file-type stuff & {:keys [read?]
                           :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/write! (io/file fs-path) stuff)
        path          (map name path)]
    (swap! db #(assoc-in % path fs-path))
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defn get
  [path & {:keys [read?]
           :or {read? true}}]
  (some->> (map name path)
           (get-in @db)
           ((if read?
              utils/read-file
              identity))))

(defn initialize-db
  []
  (reset! db {}))

(defn protein-by-id
  [protein-id]
  (with-meta
    (if-let [protein (get [:raw :uniprot :uniprotkb protein-id])]
     protein
     (let [protein (api.uniprot/uniprotkb-entry protein-id)]
       (insert! [:raw :uniprot :uniprotkb protein-id] "edn" protein)))
    api.uniprot/uniprotkb-entry-meta))

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
  [pubchem-compound-id & {:keys [read?]
                          :or {read? true}}]
  (let [sdf (pubchem/get-sdf-by-compound-id pubchem-compound-id)]
    (insert! [:raw :pubchem :compound pubchem-compound-id] "sdf" sdf :read? read?)))

(defn sdf-by-compound-id
  [pubchem-compound-id & {:keys [read?]
                          :or {read? true}}]
  (if-let [sdf-file (get [:raw :pubchem :compound pubchem-compound-id] :read? read?)]
    sdf-file
    (acquire-compound-sdf-by-id! pubchem-compound-id :read? read?)))

(defn pdbqt-file-by-compound-id
  [pubchem-compound-id]
  (let [obabel-hash (str (hash {:args    obabel/obabel-3d-conformer-args
                                :version (obabel/obabel-version)}))
        path [:processed :obabel :pdbqt pubchem-compound-id obabel-hash]]
    (if-let [pdbqt-file (get path)]
      pdbqt-file
      (let [sdf-file        (sdf-by-compound-id pubchem-compound-id :read? false)
            output-tmp-file (utils/create-temp-file "pdbqt")]
        (obabel/produce-obabel-3d-conformer! sdf-file
                                             output-tmp-file
                                             obabel/obabel-3d-conformer-args)
        (insert! path
                 "pdbqt"
                 output-tmp-file
                 :read? false)))))
