(ns schmoho.dasudopit.client.panels.pipelines.docking.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]))

#_(-> @re-frame.db/app-db :data :structure)

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
 ::current-structure-data
 :<- [:data/structures-map]
 :<- [::form]
 (fn [[structures form]]
   (let [current-structure (:current-structure form)
         source            (some-> (:source current-structure)
                                   name)]
     (if-not (= "afdb" source)
       (get-in structures [(:protein current-structure)
                           source
                           (:id current-structure)])
       (get-in structures [(:protein current-structure)
                           source])))))

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
