(ns schmoho.dasudopit.pipeline.structure
  (:require
   [schmoho.dasudopit.biotools.vina :as vina]
   [schmoho.dasudopit.formats.pdb :as formats.pdb]
   [schmoho.dasudopit.utils :as utils]))

#_(defn preprocessing-pipeline
  [protein-id
   {:keys [plddt-tail-cutoff]
    :as   params}]
  (let [params      (assoc params :obabel/version (obabel/obabel-version))
        params-hash (utils/get-hash params)]
    (if-let [pdbqt-file (db/pdbqt-by-id protein-id params-hash)]
      pdbqt-file
      (let [pdb  (db/pdb-by-id protein-id)
            path [:processed :obabel :protein :pdbqt protein-id params-hash]
            pdbqt-file
            (cond->> pdb
              plddt-tail-cutoff (formats.pdb/filter-tail-regions
                                 #(> plddt-tail-cutoff (:temperature-factor %)))
              true              (obabel/hydrogenate!)
              true              (adfr-suite/produce-pdbqt!))]
        (db.core/insert! (conj path "params") "edn" params :read? false)
        (db.core/insert! (conj path protein-id) "pdbqt" pdbqt-file :read? false)))))

#_(defn- get-active-site-locations-from-annotations
  [{:keys [active-site-name uuid]} protein-id]
  (let [protein               (db/protein-by-id protein-id)
        active-site-locations (->> protein
                                   (uniprot/active-sites active-site-name)
                                   (map uniprot/active-site-location)
                                   distinct)]
    (if (< 1 (count active-site-locations))
      (throw (ex-info "Multiple active site locations for your input. Deal with it!"
                      {:protein-id            protein-id
                       :active-site-locations active-site-locations
                       :uuid                  uuid}))
      (let [active-site-location (first active-site-locations)]
        [protein-id active-site-location]))))

(defn- prepare-box-config
  [{:keys [active-site-lookup box-size]}
   [protein-id preprocessed-file]]
  (let [active-site-residue (formats.pdb/residue preprocessed-file
                                                 (active-site-lookup protein-id))
        active-site-center  (formats.pdb/center-of-residue active-site-residue)
        box-config          (vina/vina-box-config active-site-center box-size)
        tmp-file            (utils/create-temp-file "config")]
    (spit tmp-file box-config)
    [protein-id tmp-file]))

#_(defn prepare-ligand
  [pubchem-compound-id]
  (let [params      {:obabel/args    obabel/obabel-3d-conformer-args
                     :obabel/version (obabel/obabel-version)}
        params-hash (utils/get-hash params)
        path        [:processed :obabel :ligand :pdbqt pubchem-compound-id params-hash]]
    (if-let [pdbqt-file (db/pdbqt-file-by-compound-id pubchem-compound-id params-hash)]
      pdbqt-file
      (let [sdf-file   (db/sdf-by-compound-id pubchem-compound-id :read? false)
            pdbqt-file (obabel/produce-obabel-3d-conformer! sdf-file
                                                            obabel/obabel-3d-conformer-args)]

        (db.core/insert! (conj path "params") "edn" params :read? false)
        (db.core/insert! (conj path pubchem-compound-id) "pdbqt" pdbqt-file :read? false)))))

(def params
  {:params.uniprot/protein              {:protein-ids      ["A0A0H2ZHP9"
                                                            "P02919"]
                                         :active-site-name "TRAnsPeptiDAse"}
   :params.alpha-fold/structure         {:plddt-cutoff 70}
   :params.pubchem/ligand               {:ligand-ids ["5742673"]}
   :params.taxonomy-pipeline-output/msa {:job-id nil}
   :params/docking                      {:box-size 25}
   :pipeline/uuid                       #uuid"51e4e6fa-eac0-4a2c-9490-3252a785dd6a"})

#_(defn pipeline
  [{:keys [pipeline/uuid
           params.uniprot/protein
           params.alpha-fold/structure
           params.pubchem/ligand
           params/docking]
    :as   params}]
  (let [ligand-structures     (->> (:ligand-ids ligand)
                                   (mapv (juxt identity prepare-ligand)))
        active-site-locations (->> (:protein-ids protein)
                                   (mapv (partial get-active-site-locations-from-annotations
                                                  {:active-site-name (-> protein :active-site-name)
                                                   :uuid             uuid}))
                                   (into {}))
        prepared-structures   (->> (:protein-ids protein)
                                   (mapv (fn [protein-id]
                                           [protein-id
                                            (preprocessing-pipeline
                                             protein-id
                                             {:plddt-cutoff (-> structure :plddt-cutoff)})])))
        box-configs           (->>  prepared-structures
                                    (mapv (partial prepare-box-config
                                                   {:uuid               uuid
                                                    :active-site-lookup active-site-locations
                                                    :box-size           (-> docking :box-size)})))]
    {:prepared-structures   prepared-structures
     :ligand-structures     ligand-structures
     :box-configs           box-configs
     :active-site-locations active-site-locations
     :params                params}))

#_(pipeline params)
