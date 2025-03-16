(ns schmoho.dasudopit.biodb.uniprot.mapping
  (:require
   [schmoho.dasudopit.biodb.kegg.api :as api.kegg]
   [schmoho.dasudopit.biodb.uniprot.api :as api.uniprot]
   [schmoho.dasudopit.biodb.uniprot.core :as uniprot]
   [clojure.set :as set]
   [schmoho.dasudopit.utils :as utils]))

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

(comment

  (do
    (defonce uniprot-proteome-ecoli
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))
    (defonce uniprot-proteome-pseudo
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:208963"}))
    (def kegg-proteome-ecoli
      (utils/read-file "kegg-83333.edn"))
    (def kegg-proteome-pseudo
      (utils/read-file "kegg-208963.edn")))

  (future
    (->> uniprot-proteome-pseudo
         (transduce
          (comp
           (uniprot/database-lookup-xform "KEGG")
           (map second)
           api.kegg/download-kegg-xform)
          conj
          [])
         (apply concat)
         #_(utils/write! "kegg-208963.edn")))

  (orthology-mapping-rel uniprot-proteome-ecoli
                         kegg-proteome-ecoli)

  (uniprot-kegg-id-mapping-rel uniprot-proteome-ecoli
                               kegg-proteome-ecoli
                               uniprot-proteome-pseudo
                               kegg-proteome-pseudo))
