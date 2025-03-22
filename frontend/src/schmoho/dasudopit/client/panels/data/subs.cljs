(ns schmoho.dasudopit.client.panels.data.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::data
 (fn [db _]
   (->> db :data)))


(rf/reg-sub
 :data/by-path
 :<- [::data]
 (fn [data [_ & path]]
   (get-in data path)))


(rf/reg-sub
 :data/taxons-map
 :<- [::data]
 (fn [data]
   (:taxon data)))

(rf/reg-sub
 :data/taxons
 :<- [::data]
 (fn [data]
   (->> data :taxon vals vec)))

(rf/reg-sub
 :data/taxon
 :<- [:data/taxons-map]
 (fn [taxons [_ id]]
   (get taxons id)))

(rf/reg-sub
 :data/proteomes
 :<- [:data/taxons]
 (fn [taxons]
   (map :proteome taxons)))

(rf/reg-sub
 :data/proteome
 :<- [:data/taxons]
 (fn [taxons [_ id]]
   (-> (get taxons id) :proteome)))

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
 :<- [::data]
 (fn [data]
   (->> data :ligand vals vec)))

(rf/reg-sub
 :data/ligands-map
 :<- [::data]
 (fn [data]
   (:ligand data)))

(rf/reg-sub
 :data/ligand
 :<- [:data/ligands-map]
 (fn [ligands [_ id]]
   (get ligands id)))

(rf/reg-sub
 :data/structure
 (fn [db]
   (:structure db)))

(rf/reg-sub
 :data/volcanos
 :<- [::data]
 (fn [data]
   (:volcano data)))

(rf/reg-sub
 :data/volcanos-list
 :<- [::data]
 (fn [data]
   (-> data :volcano vals vec)))

(rf/reg-sub
 :data/volcano
 :<- [:data/volcanos]
 (fn [volcanos [_ id]]
   (get volcanos id)))

(rf/reg-sub
 :results/msa
 :<- [::data]
 (fn [data]
   (->> data
        :results
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
 :<- [::data]
 (fn [data]
   (->> data
        :results
        :docking
        (mapv (fn [results]
                {:id                     (str uuid)
                 :protein-ids            (-> results :protein-ids)
                 :docking-still-running? (-> results :docking-still-running?)})))))

(rf/reg-sub
 :provision.ligand/input-model
 :<- [:forms/by-path :provision/ligand]
 (fn [form]
   (:input form)))

(rf/reg-sub
 :provision.ligand/search-result
 :<- [:data/ligands-map]
 :<- [:provision.ligand/input-model]
 (fn [[ligands input]]
   (-> ligands (get input) :search)))

(rf/reg-sub
 :provision.ligand/tab-model
 :<- [:forms/by-path :provision/ligand]
 :<- [:provision.ligand/search-result]
 (fn [[form search-result]]
   (or (:tab form)
       (-> search-result first second :meta :Title))))
