(ns schmoho.dasudopit.client.panels.data.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.utils.core :refer [cool-select-keys]]))

(rf/reg-sub
 ::data
 (fn [db _]
   (->> db :data)))

(rf/reg-sub
 :data/by-path
 :<- [::data]
 (fn [data [_ & path]]
   (get-in data path)))

;; === Taxon ===

(rf/reg-sub
 :data/taxons-map
 :<- [::data]
 (fn [data]
   (:taxon data)))

(rf/reg-sub
 :data/taxons-list
 :<- [::data]
 (fn [data]
   (->> data :taxon vals vec)))

(rf/reg-sub
 :data/taxon
 :<- [:data/taxons-map]
 (fn [taxons [_ id]]
   (get taxons id)))

(rf/reg-sub
 :data/taxon-choices
 :<- [:data/taxons-list]
 (fn [taxons]
   (conj (map #(cool-select-keys
                  %
                  {:id    [:meta :id]
                   :label [:meta :name]})
               taxons)
          {:id nil :label "-"})))

;; === Proteome ===

(rf/reg-sub
 :data/proteomes-list
 :<- [:data/taxons-list]
 (fn [taxons]
   (map :proteome taxons)))

(rf/reg-sub
 :data/proteome
 :<- [:data/taxons-map]
 (fn [taxons [_ id]]
   (-> (get taxons id) :proteome)))

;; === Protein ===

(rf/reg-sub
 :data/protein
 :<- [:data/proteomes-list]
 (fn [proteomes [_ protein-id]]
   (->> proteomes
        (mapcat
         (fn [proteome]
           (filter
            #(= protein-id (:id %))
            (:data proteome))))
        first)))

;; === Ligand ===

(rf/reg-sub
 :data/ligands-list
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

;; === Structure ===

(rf/reg-sub
 :data/structure
 (fn [db]
   (:structure db)))

;; === Volcano ===

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

