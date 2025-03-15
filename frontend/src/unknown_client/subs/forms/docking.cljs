(ns unknown-client.subs.forms.docking
  (:require
   [re-frame.core :as rf]
   [unknown-client.subs.data]))

(rf/reg-sub
 :forms/docking
 :<- [:forms/all-forms]
 (fn [forms]
   (:docking forms)))

(rf/reg-sub
 :forms.docking/taxon-model
 :<- [:forms/docking]
 (fn [form]
   (set (:taxon-model form))))

(rf/reg-sub
 :forms.docking/taxon-model-resolved
 :<- [:forms.docking/taxon-model]
 :<- [:data/taxons-map]
 (fn [[model taxons]]
   (map #(get taxons %) model)))

(rf/reg-sub
 :forms.docking/ligand-model
 :<- [:forms/docking]
 (fn [form]
   (set (:ligand-model form))))

(rf/reg-sub
 :forms.docking/selected-proteins-model
 :<- [:forms/docking]
 (fn
   [form]
    (:selected-proteins-model form)))

(rf/reg-sub
 :forms.docking/selected-protein-for-taxon
 :<- [:forms.docking/selected-proteins-model]
 (fn
   [model [_ id]]
   (get model id)))

(rf/reg-sub
 :forms.docking/selected-protein-for-taxon-resolved
 :<- [:forms.docking/selected-proteins-model]
 :<- [:data/proteomes]
 (fn
   [[model proteomes] [_ taxon-id]]
   (let [selected-protein (:id (get model taxon-id))
         proteome         (get proteomes taxon-id)
         proteome (zipmap
                   (map :primaryAccession proteome)
                   proteome)]
     (get proteome selected-protein))))

(rf/reg-sub
 :forms.docking/selected-proteins-ids
 :<- [:forms.docking/selected-proteins-model]
 (fn [model]
   (->> model vals (map :id))))

(rf/reg-sub
 :forms.docking/selected-proteins-model-all
 :<- [:forms/docking]
 (fn
   [form]
   (:selected-proteins-model form)))


