(ns schmoho.dasudopit.client.panels.pipelines.docking.subs
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.utils :as utils :refer [cool-select-keys]]))

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
 :forms.docking.part-1/taxon-model
 :<- [:forms.docking/input-model]
 (fn [form]
   (-> form :taxon keys set)))

(rf/reg-sub
 :forms.docking.part-1/ligand-model
 :<- [:forms.docking/input-model]
 (fn [form]
   (-> form :ligand set)))

(rf/reg-sub
 :forms.docking.part-1/valid?
 :<- [:forms.docking.part-1/taxon-model]
 :<- [:forms.docking.part-1/ligand-model]
 (fn [[taxon ligand]]
   (and (not-empty taxon)
        (not-empty ligand)
        (some? taxon)
        (some? ligand))))

;; === Part 2 ===

(rf/reg-sub
 :forms.docking.part-2/selected-protein-for-taxon-resolved
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
 :forms.docking.part-2/valid?
 :<- [:forms.docking/input-model]
 :<- [:data/structures]
 (fn [[input-model structures]]
   (let [protein-ids (->> input-model :taxon vals (map (comp :id :protein)))]
     (and (every? some? (map #(get structures %) protein-ids))
          (pos? (count protein-ids))))))


;; (rf/reg-sub
;;  :forms.docking.part-2/show-modal?
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
