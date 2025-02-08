(ns clustalo
  (:require
   [clojure.java.shell :as sh]))

#_(sh/sh "clustalo" "--help")

(defn uniprot-proteins->fasta
  [uniprot-proteins]
  )



;; (def msa (json/parse-string (slurp "/home/rick/Desktop/asdf.json") true))


;; (keys msa)
;; ;; => (:seqs :appSettings :seqGroups :alignAnnotation :svid :seqFeatures)
;; (:seqFeatures msa)

;; (-> msa
;;     (assoc :seqFeatures [])
;;     (update :seqs (partial map #(assoc % :id "")))
;;     (json/generate-string)
;;     (->> (spit "/home/rick/Desktop/asdf2.json")))

