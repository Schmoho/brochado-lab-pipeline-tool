(ns pipeline.structure
  (:require
   [biodb.uniprot.core :as uniprot]
   [biotools.adfr-suite :as adfr-suite]
   [biotools.obabel :as obabel]
   [biotools.vina :as vina]
   [clojure.string :as str]
   [db :as db]
   [formats.pdb :as formats.pdb]
   [utils :as utils]))

(defn preprocessing-pipeline
  ([pdb] (preprocessing-pipeline pdb nil))
  ([pdb plddt-tail-cutoff]
   (let [intermediate-file (utils/create-temp-file "pdb")
         result-file       (utils/create-temp-file "pdbqt")]
     (if plddt-tail-cutoff
       (->> pdb
            (str/split-lines)
            (formats.pdb/filter-tail-regions
             #(> plddt-tail-cutoff (:temperature-factor %)))
            (str/join "\n")
            (spit intermediate-file))
       (spit intermediate-file pdb))
     (obabel/hydrogenate! (.getAbsolutePath intermediate-file))
     (adfr-suite/produce-pbdqt!
      (.getAbsolutePath intermediate-file)
      (.getAbsolutePath result-file))
     {:preprocessed-pdb   intermediate-file
      :preprocessed-pdbqt result-file})))

(defn- get-active-site-locations-from-annotations
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

(defn- prepare-structure
  [{:keys [plddt-cutoff]} protein-id]
  (let [pdb (db/pdb-by-id protein-id)
        {:keys [preprocessed-pdbqt]} (preprocessing-pipeline pdb
                                                             plddt-cutoff)]
    [protein-id  preprocessed-pdbqt]))

(defn- prepare-box-config
  [{:keys [uuid active-site-lookup box-size]}
   [protein-id preprocessed-file]]
  (let [active-site-residue (formats.pdb/residue
                             (formats.pdb/parsed-pdb preprocessed-file)
                             (active-site-lookup protein-id))
        active-site-center  (formats.pdb/center-of-residue active-site-residue)
        box-config          (vina/vina-box-config active-site-center box-size)
        tmp-file            (utils/create-temp-file "config")]
    (spit tmp-file box-config)
    [protein-id tmp-file]))

(def params
  {:params.uniprot/protein              {:protein-ids      ["A0A0H2ZHP9"
                                                            "P02919"]
                                         :active-site-name "TRAnsPeptiDAse"}
   :params.alpha-fold/structure         {:plddt-cutoff 70}
   :params.pubchem/ligand               {:ligand-ids ["5742673"]}
   :params.taxonomy-pipeline-output/msa {:job-id nil}
   :params/docking                      {:box-size 25}
   :pipeline/uuid                       #uuid"51e4e6fa-eac0-4a2c-9490-3252a785dd6a"})

(defn pipeline
  [{:keys [pipeline/uuid
           params.uniprot/protein
           params.alpha-fold/structure
           params.pubchem/ligand
           params/docking]
    :as params}]
  (let [protein-ids              (-> protein :protein-ids)
        active-site-name         (-> protein :active-site-name)
        plddt-cutoff             (-> structure :plddt-cutoff)
        active-site-locations    (->> protein-ids
                                      (mapv (partial get-active-site-locations-from-annotations
                                                     {:active-site-name active-site-name
                                                      :uuid             uuid}))
                                      (into {}))
        prepared-structures      (mapv (partial prepare-structure
                                                {:uuid         uuid
                                                 :plddt-cutoff plddt-cutoff})
                                       protein-ids)
        ligand-structures        (mapv (juxt identity db/pdbqt-file-by-compound-id)
                                       (-> ligand :ligand-ids))
        box-configs              (mapv (partial prepare-box-config
                                                {:uuid               uuid
                                                 :active-site-lookup active-site-locations
                                                 :box-size           (-> docking :box-size)})
                                       prepared-structures)]
    {:prepared-structures prepared-structures
     :ligand-structures   ligand-structures
     :box-configs         box-configs
     :active-site-locations active-site-locations}))

#_(pipeline params)
