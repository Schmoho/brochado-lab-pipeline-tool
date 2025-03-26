(ns schmoho.dasudopit.client.panels.pipelines.docking.subs
  (:require
   [re-frame.core :as rf]))

;; === Top Level ===

(rf/reg-sub
 :forms/docking
 :<- [:forms/all-forms]
 (fn [forms]
   (:docking forms)))

(rf/reg-sub
 :forms.docking/input-model
 :<- [:forms/docking]
 (fn [form]
   (:input-model form)))

;; === Part 1 ===

(rf/reg-sub
 :forms.docking.provide-data/taxon-model
 :<- [:forms.docking/input-model]
 (fn [form]
   (-> form :taxon keys set)))

(rf/reg-sub
 :forms.docking.provide-data/ligand-model
 :<- [:forms.docking/input-model]
 (fn [form]
   (-> form :ligand set)))

(rf/reg-sub
 :forms.docking.provide-data/valid?
 :<- [:forms.docking.provide-data/taxon-model]
 :<- [:forms.docking.provide-data/ligand-model]
 (fn [[taxon ligand]]
   (and (not-empty taxon)
        (not-empty ligand)
        (some? taxon)
        (some? ligand))))

;; === Part 2 ===

(rf/reg-sub
 :forms.docking.choose-binding-sites/selected-protein-for-taxon-resolved
 :<- [:forms.docking/input-model]
 :<- [:data/proteomes]
 (fn
   [[input-model proteomes] [_ taxon-id]]
   (let [selected-protein (-> input-model :taxon (get taxon-id) :protein)
         proteome         (get proteomes taxon-id)
         proteome (zipmap
                   (map :primaryAccession proteome)
                   proteome)]
     (get proteome selected-protein))))


(rf/reg-sub
 :forms.docking.choose-binding-sites/valid?
 :<- [:forms.docking/input-model]
 :<- [:data/structures]
 (fn [[input-model structures]]
   (let [protein-ids (->> input-model :taxon vals (map (comp :id :protein)))]
     (and (every? some? (map #(get structures %) protein-ids))
          (pos? (count protein-ids))))))


;; (rf/reg-sub
;;  :forms.docking.choose-binding-sites/show-modal?
;;  :<- [:forms.docking/input-model]
;;  (fn [input-model]
;;    (let [show-modal? (->> input-model :taxon vals (map (comp :id :protein)))]
;;      (and (every? some? (map #(get structures %) protein-ids))
;;           (pos? (count protein-ids))))))

;; === Part 4 ===

(rf/reg-sub
 :forms.docking.part-4/plddt-cutoff
 :<- [:forms.docking/input-model]
 (fn [input-model [_ taxon-id]]
   (-> input-model
       :taxon
       (get taxon-id)
       :plddt-cutoff)))

(rf/reg-sub
 :forms.docking.part-4/plddt-viewer
 :<- [:forms.docking/input-model]
 (fn [input-model [_ taxon-id]]
   (-> input-model
       :taxon
       (get taxon-id)
       :viewer
       :plddt)))
