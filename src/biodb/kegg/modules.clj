(ns kegg.modules)

;;;; PATHWAY MAP ZEUG

#_(defn modules-from-pathways
  [pathways]
  (->> pathways
       (map (comp (partial map first)
                  #(get % "MODULE")))
       (apply concat)
       set
       (map (comp parse-kegg-get-result
                  kegg-get
                  #(subs % 4)))))

#_(first (modules-from-pathways pseudo-paths))
