(ns unknown-client.subs.data
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :data/taxons
 (fn [db _]
   (->> db :data :taxon vals vec)))

(rf/reg-sub
 :data/taxon
 (fn [db [_ id]]
   (-> db :data :taxon (get id))))

(rf/reg-sub
 :data/proteomes
 (fn [db [_ id]]
   (-> db :data :proteome)))

(rf/reg-sub
 :data/proteome
 (fn [db [_ id]]
   (-> db :data :proteome (get id))))

(rf/reg-sub
 :data/ligands
 (fn [db _]
   (->> db :data :ligand vals vec)))

(rf/reg-sub
 :data/ligand
 (fn [db [_ id]]
   (-> db :data :ligand (get id))))



(rf/reg-sub
 :msa/results
 (fn [db _]
   (->> db
        :msa
        :results
        (mapv (fn [[uuid {:keys [params.uniprot/taxonomy
                                 params.uniprot/uniref
                                 params.uniprot/blast
                                 params.uniprot/protein]
                          :as result}]]
                {:id                   (str uuid)
                 :protein-ids          (-> protein :protein-ids)
                 :gene-names           (-> protein :gene-names)
                 :blast-still-running? (-> result :blast-still-running?)})))))
