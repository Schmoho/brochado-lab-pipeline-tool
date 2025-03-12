(ns unknown-client.subs.data
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :data/raw
 (fn [db _]
   (->> db :data :raw)))

(rf/reg-sub
 :data/taxons
 :<- [:data/raw]
 (fn [raw]
   (->> raw :taxon vals vec)))

(rf/reg-sub
 :data/taxon
 :<- [:data/raw]
 (fn [raw [_ id]]
   (-> raw :taxon (get id))))

(rf/reg-sub
 :data/proteome
 :<- [:data/raw]
 (fn [raw [_ id]]
   (-> raw :proteome (get id))))

(rf/reg-sub
 :data/ligands
 :<- [:data/raw]
 (fn [raw]
   (->> raw :ligand vals vec)))

(rf/reg-sub
 :data/ligand
 :<- [:data/raw]
 (fn [raw [_ id]]
   (-> raw :ligand (get id))))

(rf/reg-sub
 :data/input
 (fn [db _]
   (->> db :data :input)))

(rf/reg-sub
 :data/volcanos
 :<- [:data/input]
 (fn [input]
   (->> input :volcano vals vec)))

(rf/reg-sub
 :data/results
 (fn [db _]
   (->> db :data :results)))

(rf/reg-sub
 :msa/results
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
