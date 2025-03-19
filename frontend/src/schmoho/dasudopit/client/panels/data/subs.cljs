(ns schmoho.dasudopit.client.panels.data.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :data/raw
 (fn [db _]
   (->> db :data :raw)))

(rf/reg-sub
 :data/taxons-map
 :<- [:data/raw]
 (fn [raw]
   (->> raw :taxon)))

(rf/reg-sub
 :data/taxons
 :<- [:data/raw]
 (fn [raw]
   (->> raw :taxon vals vec)))

(rf/reg-sub
 :data/taxon
 :<- [:data/taxons-map]
 (fn [taxons [_ id]]
   (get taxons id)))

(rf/reg-sub
 :data/proteomes
 :<- [:data/raw]
 (fn [raw]
   (-> raw :proteome)))

(rf/reg-sub
 :data/proteome
 :<- [:data/proteomes]
 (fn [proteomes [_ id]]
   (get proteomes id)))

(rf/reg-sub
 :data/protein
 :<- [:data/proteomes]
 (fn [proteomes [_ protein-id]]
   (->> proteomes
        vals
        (mapcat (fn [proteome]
               (filter
                #(= protein-id (:primaryAccession %))
                proteome)))
        first)))

(rf/reg-sub
 :data/ligands
 :<- [:data/raw]
 (fn [raw]
   (->> raw :ligand vals vec)))

(rf/reg-sub
 :data/ligands-map
 :<- [:data/raw]
 (fn [raw]
   (:ligand raw)))

(rf/reg-sub
 :data/ligand
 :<- [:data/ligands-map]
 (fn [ligands [_ id]]
   (get ligands id)))

(rf/reg-sub
 :data/structures
 :<- [:data/raw]
 (fn [raw]
   (->> raw :structure)))

(rf/reg-sub
 :data/input
 (fn [db _]
   (->> db :data :input)))

(rf/reg-sub
 :data/volcanos
 :<- [:data/input]
 (fn [input]
   (:volcano input)))

(rf/reg-sub
 :data/volcanos-list
 :<- [:data/input]
 (fn [input]
   (-> input :volcano vals vec)))

(rf/reg-sub
 :data/volcano
 :<- [:data/volcanos]
 (fn [volcanos [_ id]]
   (get volcanos id)))

(rf/reg-sub
 :data/results
 (fn [db _]
   (->> db :data :results)))

(rf/reg-sub
 :results/msa
 :<- [:data/results]
 (fn [results]
   (->> results
        :msa
        (mapv (fn [[uuid {:keys [params.uniprot/taxonomy
                                 params.uniprot/uniref
                                 params.uniprot/blast
                                 params.uniprot/protein]
                          :as result}]]
                {:id                   (str uuid)
                 :protein-ids          (-> protein :protein-ids)
                 :gene-names           (-> protein :gene-names)
                 :blast-still-running? (-> result :blast-still-running?)})))))

(rf/reg-sub
 :results/docking
 :<- [:data/results]
 (fn [results]
   (->> results
        :docking
        (mapv (fn [results]
                {:id                     (str uuid)
                 :protein-ids            (-> results :protein-ids)
                 :docking-still-running? (-> results :docking-still-running?)})))))
