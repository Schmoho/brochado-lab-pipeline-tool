(ns db.api
  (:require
   [db.core :as db]
   [biodb.afdb :as afdb]
   [biodb.pubchem :as pubchem]
   [biodb.uniprot.api :as api.uniprot]))

(defn protein-by-id
  [protein-id]
  (with-meta
    (if-let [protein (db/get [:raw :uniprot :uniprotkb protein-id])]
     protein
     (let [protein (api.uniprot/uniprotkb-entry protein-id)]
       (db/insert! [:raw :uniprot :uniprotkb protein-id] "edn" protein)))
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
        {:pdb (db/insert! [:raw :afdb :pdb protein-id] "pdb" pdb)
         :cif (db/insert! [:raw :afdb :cif protein-id] "cif" cif)}))))

(defn pdb-by-id
  [protein-id]
  (if-let [pdb-file (db/get [:raw :afdb :pdb protein-id])]
    pdb-file
    (:pdb (acquire-structures-by-id! protein-id))))

(defn pdbqt-by-id
  [protein-id params-hash]
  (tap> [protein-id params-hash])
  (db/get [:processed :obabel :protein :pdbqt protein-id params-hash]))

(defn- acquire-compound-sdf-by-id!
  [pubchem-compound-id & {:keys [read?]
                          :or {read? true}}]
  (let [sdf (pubchem/get-sdf-by-compound-id pubchem-compound-id)]
    (db/insert! [:raw :pubchem :compound pubchem-compound-id] "sdf" sdf :read? read?)))

(defn sdf-by-compound-id
  [pubchem-compound-id & {:keys [read?]
                          :or {read? true}}]
  (if-let [sdf-file (get [:raw :pubchem :compound pubchem-compound-id] :read? read?)]
    sdf-file
    (acquire-compound-sdf-by-id! pubchem-compound-id :read? read?)))

(defn pdbqt-file-by-compound-id
  [pubchem-compound-id parameter-hash]
  (db/get (conj [:processed :obabel :ligand :pdbqt pubchem-compound-id parameter-hash]
                pubchem-compound-id)
          :read? false))

(defn write-result!
  [job-id result-map]
  )
