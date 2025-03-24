(ns schmoho.dasudopit.biodb.kegg.comparison)


;; (defn gene-orthology-comparison
;;   [gene-map1 gene-map2]
;;   (let [orthology1 (gene-orthology-lookup gene-map1)
;;         orthology2 (gene-orthology-lookup gene-map2)]
;;     {:gene-count1                 (count gene-map1)
;;      :gene-count2                 (count gene-map2)
;;      :mappable-gene-count-1       (count (filter some? (vals orthology1)))
;;      :mappable-gene-count-2       (count (filter some? (vals orthology2)))
;;      :mappable-gene-overlap-count (count(set/intersection
;;                                          (set (filter some? (vals orthology1)))
;;                                          (set (filter some? (vals orthology2)))))}))


;; (defn pathway-orthology-comparison
;;   [pathway-map1 pathway-map2]
;;   (let [orthology1 (map (comp first :KO_PATHWAY) (vals pathway-map1))
;;         orthology2 (map (comp first :KO_PATHWAY) (vals pathway-map2))]
;;     {:pathway-count1                 (count pathway-map1)
;;      :pathway-count2                 (count pathway-map1)
;;      :mappable-pathway-count-1       (count orthology1)
;;      :mappable-pathway-count-2       (count orthology2)
;;      :mappable-pathway-overlap-count (count(set/intersection
;;                                             (set (filter some? orthology1))
;;                                             (set (filter some? orthology2))))}))
