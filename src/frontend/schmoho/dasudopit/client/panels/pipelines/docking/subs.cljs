(ns schmoho.dasudopit.client.panels.pipelines.docking.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]))

;; === Top Level ===

(rf/reg-sub
 ::form
 :<- [::forms/all-forms]
 (fn [forms]
   (:docking forms)))

;; (rf/reg-sub
;;  :forms.docking/input-model
;;  :<- [:forms/docking]
;;  (fn [form]
;;    (:input-model form)))

;; === Part 1 ===

(rf/reg-sub
 ::selected-taxons-model
 :<- [::form]
 (fn [form]
   (-> form :taxon keys set)))

(rf/reg-sub
 :selected-ligands-model
 :<- [::form]
 (fn [form]
   (-> form :ligand set)))

(rf/reg-sub
 ::selected-structures-model
 :<- [:forms/by-path :docking]
 :<- [:forms.docking.provide-data/taxon-model]
 (fn [[form taxon-model]]
   (or (:tab form)
       (-> taxon-model first))))

(rf/reg-sub
 :form.docking.provide-data/tabbed-taxon-model
 :<- [:forms.docking.provide-data/tab-model]
 :<- [:data/taxons-map]
 (fn [[tab-model taxons]]
   (get taxons tab-model)))

(rf/reg-sub
 ::current-protein
 :<- [::form]
 (fn [form [_ taxon]]
   (let [taxon-id (-> taxon :meta :id)]
     (-> form :taxon (get taxon-id) :protein))))

(rf/reg-sub
 :forms.docking.provide-data/current-protein-structure
 :<- [:forms.docking.provide-data/tab-model]
 :<- [:forms/by-path :docking :input-model :taxon]
 :<- [:data/structures-map]
 (fn [[tab taxons-input structures]]
   (let [protein (get-in taxons-input [tab :protein])
         structure (get structures (:id protein))]
     structure)))

(rf/reg-sub
 ::provided-data-valid?
 ;; :<- [::taxon-model]
 ;; :<- [::ligand-model]
 (fn [_]
   #_(and (not-empty taxon)
        (not-empty ligand)
        (some? taxon)
        (some? ligand))
   true))

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
