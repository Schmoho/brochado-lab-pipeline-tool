(ns schmoho.dasudopit.client.panels.data.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.utils.core :refer [cool-select-keys]]
   [schmoho.biodb.uniprot.core :as uniprot]))

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
   (->> data :taxon vals ((partial sort-by (comp :name :meta))) vec)))

(rf/reg-sub
 :data/taxon
 :<- [:data/taxons-map]
 (fn [taxons [_ id]]
   (get taxons id)))

(rf/reg-sub
 :data/taxon-choices
 :<- [:data/taxons-list]
 (fn [taxons [_ optional?]]
   (let [choices (map #(cool-select-keys
                        %
                        {:id    [:meta :id]
                         :label [:meta :name]})
                      taxons)]
     (if optional?
       (conj choices {:id nil :label "-"})
       choices))))

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
        (keep (comp
               (fn [proteome]
                 (get proteome protein-id))
               :data))
        first)))

#_@(rf/subscribe [:data/structures-map])

(rf/reg-sub
 :data/structure-choices
 :<- [:data/structures-map]
 (fn [structures [_ protein]]
   (let [structure (get structures (:id protein))
         choices   (->> structure
                        (mapcat
                         (fn [[group stuff]]
                           (if (:meta stuff)
                             [{:group group
                               :id    (:meta stuff)
                               :label (-> stuff :meta :name)}]
                             (map (fn [[_ {:keys [meta]}]]
                                    {:group group
                                     :id    meta
                                     :label (str (:name meta))})
                                  stuff)))))]
     choices)))


;; === Ligand ===

(rf/reg-sub
 :data/ligands-list
 :<- [::data]
 (fn [data]
   (->> data :ligand vals ((partial sort-by (comp :title :meta)))  vec)))

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
 :data/ligand-choices
 :<- [:data/ligands-list]
 (fn [ligands]
   (->> ligands
        (mapv #(cool-select-keys
                %
                {:label [:meta :title]
                 :id    [:meta :cid]})))))

;; === Structure ===

(rf/reg-sub
 :data/structures-map
 :<- [::data]
 (fn [data]
   (:structure data)))

(rf/reg-sub
 :data/structures-list
 :<- [::data]
 (fn [data]
   (->> (:structure data)
        (tree-seq map? vals)
        (filter (fn [m]
                  (and (map? m)
                       (:meta m)
                       (not= :afdb (-> m :meta :source)))))
        (mapv :meta))))

(rf/reg-sub
 :data/structure
 :<- [::data]
 (fn [data [_ protein-id]]
   (-> data
       :structure
       (get protein-id))))

(rf/reg-sub
 :data/structure-by-type
 :<- [::data]
 (fn [data [_ protein-id type id]]
   (-> data
       :structure
       (get protein-id)
       (get (name type))
       (get id))))

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
   (-> data :volcano vals ((partial sort-by (comp :name :meta))) vec)))

(rf/reg-sub
 :data/volcano
 :<- [:data/volcanos]
 (fn [volcanos [_ id]]
   (get volcanos id)))

