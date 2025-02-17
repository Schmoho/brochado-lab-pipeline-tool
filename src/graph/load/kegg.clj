(ns graph.load.kegg
  (:require
   [clojure.tools.logging :as log]
   [graph.cypher :as cypher]
   [graph.mapping.kegg :as mapping]))

(defn load-cds!
  [connection cds]
  (log/debug "Loading CDS into DB.")
  (->> cds
       (mapping/cds->cds-node)
       (cypher/merge-node-with-rels-by-id! connection)))



;; orthology
;; pathways
;; orthologous pathways
;; dblinks
;; organism

;; (kegg.parser/parse-kegg-get-result (kegg.api/get "ko:K10941"))

;; (kegg.api/get "pathway:pae02020")

;; (def stuuuuff
;;   (->> (filter
;;        #(not= "CDS" (second %))
;;        (map
;;         #(str/split % #"\t")
;;         (str/split-lines  (kegg.api/list "pae"))))
;;       (group-by second)
;;       (vals)
;;       (map (comp kegg.api/get ffirst))
;;       doall))

;; (defn entry-type
;;   [entry]
;;   (-> entry
;;       :entry
;;       second
;;       str/lower-case
;;       keyword))

;; (->> (map (comp  kegg.parser/parse-kegg-get-result)
;;           stuuuuff))
