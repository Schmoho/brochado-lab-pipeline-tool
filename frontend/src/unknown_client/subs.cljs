(ns unknown-client.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-route
 (fn [db _]
   (:active-route db)))

(re-frame/reg-sub
 ::active-panel
 :<- [::active-route]
 (fn [active-route]
   (:handler active-route)))

(re-frame/reg-sub
 ::active-route-params
 :<- [::active-route]
 (fn [active-route]
   (:route-params active-route)))

(re-frame/reg-sub
 :taxonomic-comparison/form
 (fn [db _]
   (-> db :taxonomic-comparison :form)))

(re-frame/reg-sub
 ::tour
 (fn [db _]
   (-> db :tour)))

(re-frame/reg-sub
 :taxonomic-comparison/results
 (fn [db _]
   (->> db
        :taxonomic-comparison
        :results
        (mapv (fn [[uuid {:keys [params.uniprot/taxonomy
                                 params.uniprot/uniref
                                 params.uniprot/blast
                                 params.uniprot/protein]
                          :as result}]]
                {:id                   (str uuid)
                 :protein-ids          (-> protein :protein-ids str)
                 :gene-names           (-> protein :gene-names str)
                 :blast-still-running? (-> result :blast-still-running? str)})))))

(re-frame/reg-sub
 :data/taxons
 (fn [db _]
   (->> db :data :taxon vals vec)))

(re-frame/reg-sub
 :data/taxon
 (fn [db [_ id]]
   (-> db :data :taxon (get id))))

(re-frame/reg-sub
 :data/proteome
 (fn [db [_ id]]
   (-> db :data :proteome (get id))))

(re-frame/reg-sub
 :data/ligands
 (fn [db _]
   (->> db :data :ligand vals vec)))

(re-frame/reg-sub
 :data/ligand
 (fn [db [_ id]]
   (-> db :data :ligand (get id))))
