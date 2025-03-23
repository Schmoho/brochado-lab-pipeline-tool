(ns schmoho.dasudopit.client.panels.results.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :results/msa
 :<- [::data]
 (fn [data]
   (->> data
        :results
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

(rf/reg-sub
 :results/docking
 :<- [::data]
 (fn [data]
   (->> data
        :results
        :docking
        (mapv (fn [results]
                {:id                     (str uuid)
                 :protein-ids            (-> results :protein-ids)
                 :docking-still-running? (-> results :docking-still-running?)})))))
