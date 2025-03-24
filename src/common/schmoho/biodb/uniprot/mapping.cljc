(ns schmoho.biodb.uniprot.mapping
  (:require
   [schmoho.biodb.uniprot.core :as uniprot]
   [clojure.set :as set]))


  (defn orthology-mapping-rel
    [uniprot-proteome kegg-proteome]
    (->> (set/join
          (->> uniprot-proteome
               (transduce
                (comp
                 (uniprot/database-lookup-xform "KEGG")
                 (map (partial zipmap [:uniprot-id :kegg-id])))
                conj
                []))
          (->> kegg-proteome
               (map (comp
                     #(update % :orthology ffirst)
                     #(select-keys % [:kegg-id :orthology])
                     #(set/rename-keys % {:id :kegg-id})))))))

  (defn uniprot-kegg-id-mapping-rel
    [uniprot-proteome-1 kegg-proteome-1
     uniprot-proteome-2 kegg-proteome-2]
    (->> (set/join
          (set (filter (comp not-empty :orthology)
                       (set/rename (orthology-mapping-rel
                                    uniprot-proteome-1
                                    kegg-proteome-1)
                                   {:uniprot-id :uniprot-id-1
                                    :kegg-id    :kegg-id-1})))
          (set (filter (comp not-empty :orthology)
                       (set/rename (orthology-mapping-rel
                                    uniprot-proteome-2
                                    kegg-proteome-2)
                                   {:uniprot-id :uniprot-id-2
                                    :kegg-id    :kegg-id-2}))))))
