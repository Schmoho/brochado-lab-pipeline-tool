(ns kegg.maps)


;; (->> (take 1 pseudo-paths)
;;      (map (comp (partial map first) #(get % "PATHWAY_MAP"))))

;; (->> (pathways pseudo-data)
;;      (map
;;       (comp
;;        (fn [_]
;;          (Thread/sleep 500))
;;        #(spit (str "resources/kegg/kgml/" (first %) ".xml") (second %))
;;        (juxt identity #(kegg-get (str % "/kgml"))))))

;; (kegg-get (str (first) "/kgml"))
;; "pau00620"

;; (def conf (kegg-get (str (first (pathways pseudo-data)) "/kgml")))
