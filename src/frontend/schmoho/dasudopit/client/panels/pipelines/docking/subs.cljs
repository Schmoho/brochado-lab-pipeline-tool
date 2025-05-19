(ns schmoho.dasudopit.client.panels.pipelines.docking.subs
  (:require
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.panels.data.subs :as subs]
   [clojure.string :as str]))

#_(-> @re-frame.db/app-db :data :structure)

;; === Top Level ===

(rf/reg-sub
 ::form
 :<- [::forms/all-forms]
 (fn [forms]
   (:docking forms)))



;; === Provide Data ===

(rf/reg-sub
 ::current-protein-data
 :<- [::subs/data]
 :<- [::form]
 (fn [[data form]]
   (let [current-taxon    (:current-taxon form)
         selected-protein (-> form :selected-proteins (get current-taxon) :id)]
     (-> data
         :taxon
         (get current-taxon)
         :proteome
         :data
         (get selected-protein)))))

(rf/reg-sub
 ::current-structure-data
 :<- [:data/structures-map]
 :<- [::form]
 (fn [[structures form]]
   (let [current-taxon     (:current-taxon form)
         current-structure (-> form :selected-structures (get current-taxon))
         current-protein   (-> form :selected-proteins (get current-taxon) :id)
         source            (some-> (:source current-structure)
                                   name)]
     (when (= (:protein current-structure) current-protein)
       (if-not (= "afdb" source)
         (get-in structures [(:protein current-structure)
                             source
                             (:id current-structure)])
         (get-in structures [(:protein current-structure)
                             source]))))))

(rf/reg-sub
 ::current-binding-site
 :<- [::form]
 (fn [form]
   (let [current-taxon          (:current-taxon form)]
     (-> form :selected-binding-sites (get current-taxon)))))

;; (-> @re-frame.db/app-db :forms :docking :current-taxon)

;; (rf/subscribe ::form)

(rf/reg-sub
 ::provided-data-valid?
 :<- [::form]
 (fn [form]
   (let [taxa       (:selected-taxons form)
         proteins   (:selected-proteins form)
         structures (:selected-structures form)]
     (and (not-empty taxa)
          (every? #(-> proteins (get %) some?) taxa)
          (every? #(-> structures (get %) some?) taxa)
          true))))

#_(-> @(rf/subscribe [::form]) :current-taxon)

;; === Preprocessing ===

(rf/reg-sub
 ::plddt-cutoff
 :<- [::form]
 (fn [form]
   (or (let [taxon-id (:current-taxon form)]
      (-> form
          :plddt-cutoffs
          (get taxon-id)))
       80)))

(rf/reg-sub
 ::plddt-cutoff-indices
 :<- [::current-structure-data]
 :<- [::plddt-cutoff]
 (fn [[structure cutoff]]
   (when-let [pdb (:structure structure)]
     (let [first-atom    (str/index-of pdb "ATOM")
           last-atom-end (+ (str/last-index-of pdb "ATOM") 80)
           atoms         (subs pdb first-atom last-atom-end)
           atoms         (->> (str/split-lines atoms)
                              (map #(str/split % #"\s+")))]
       {:start (or (some-> (drop-while #(or (> 10 (count %))
                                            (>= cutoff (nth % 10)))
                                       atoms)
                           first
                           (nth 5)
                           parse-long)
                   90000)
        :end (or (some-> (drop-while #(or (> 10 (count %))
                                          (>= cutoff (nth % 10)))
                                     (reverse atoms))
                         first
                         (nth 5)
                         parse-long)
                 0)}))))
