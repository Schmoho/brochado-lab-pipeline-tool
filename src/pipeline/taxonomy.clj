(ns pipeline.taxonomy
  (:require
   [biotools.clustalo :as clustalo]
   [biodb.uniprot.api :as api.uniprot]
   [biodb.uniprot.core :as uniprot.core]
   [camel-snake-kebab.core :as csk]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [graph.accrete.core]
   [graph.mapping.uniprot.blast :as mapping.uniprot.blast]
   [graph.cypher :as cypher]
   [biodb.uniprot.blast :as blast]))

;; protein-id
;; ;; -> taxon-id
;; ;; ;; -> lineage
;; ;; ;; ;; -> species-level
;; ;; ;; ;; ;; -> ancestor:species-id
;; ;; ;; ;; ;; ;; -> page through
;; ;; ;; ;; ;; ;; ;; -> gene:geneName + taxonomy
;; ;; -> uniref-clusters
;; ;; ;; -> filter by taxonomic lineage

(defn pipeline
  [protein-id gene-name]
  (let [protein               (-> protein-id (api.uniprot/uniprotkb-entry))
        blast                 (blast/run-blast-query!
                               {:blast/database       :uniprot-bacteria
                                :blast/query-sequence (-> protein :sequence :value)})
        blast-proteins        (mapv (comp api.uniprot/uniprotkb-entry :id) @blast)
        taxon                 (-> protein :organism :taxonId (api.uniprot/taxonomy-entry))
        species               (or (->> pa :lineage (drop-while #(not= (:rank %) "species")) first)
                                  taxon)
        proteins-by-gene-name (api.uniprot/uniprotkb-search
                               {:query
                                (format "gene:%s AND taxonomy_name:%s"
                                        gene-name
                                        (:scientificName species))})
        uniref-clusters       (-> protein :primaryAccession api.uniprot/uniref-by-protein-id)
        get-clusters          (fn [type]
                                (->> uniref-clusters
                                     (map :entryType)
                                     (filter #(= type %))
                                     (map (comp :members api.uniprot/uniref-entry))))
        get-cluster-proteins  (fn [cluster]
                                (let [{:keys [uni-prot-kb-id uni-parc]}
                                      (-> (group-by :memberIdType cluster)
                                          (update-keys csk/->kebab-case-keyword))]
                                  {:uniprotkb (->> uni-prot-kb-id
                                                   (mapcat :accessions)
                                                   (distinct)
                                                   (mapv api.uniprot/uniprotkb-entry))
                                   :uniparc   (->> uni-parc
                                                   (map :memberId)
                                                   (distinct)
                                                   (mapv api.uniprot/uniparc-entry))}))
        uniref-90             (->> (get-clusters "UniRef90") (map get-cluster-proteins))
        uniref-100            (->> (get-clusters "UniRef100") (map get-cluster-proteins))]
    #_(->> 
         (map (comp formats.fasta/->fasta
                    #(or (uniprot.core/domain-restricted-protein "transpeptidase" %) %)))
         (clustalo/clustalo))
    ))

#_(def pa-mrcb
    (api.uniprot/uniprotkb-entry "A0A0H2ZHP9"))

#_(def pa
    (-> pa-mrcb :organism :taxonId (api.uniprot/taxonomy-entry)))

#_(defn upstream-species
    [taxon]
    (->> pa
         :lineage
         (drop-while #(not= (:rank %) "species"))
         first))

#_(def pa-lineage
    (->> (upstream-species pa)
         :taxonId
         (api.uniprot/downstream-lineage)))

#_(def all-pa-mrcbs-by-gene-name
    (api.uniprot/uniprotkb-search {:query (format "gene:%s AND taxonomy_name:%s"
                                                  "mrcb" "Pseudomonas aeruginosa")}))

#_(def mrcb-uniref-cluster
    (api.uniprot/uniref-by-protein-id (:primaryAccession pa-mrcb)))

#_(def mrcb-uniref-90
    (api.uniprot/uniref-entry #_"UniRef50_Q4K603"
                              "UniRef90_G3XD31"
                              #_"UniRef100_A0A0U4NUB5"))

#_(def mrcb-uniref-90-entries
  (let [{:keys [uni-prot-kb-id uni-parc]}
          (as-> mrcb-uniref-90 $
            (:members $)
            (group-by :memberIdType $)
            (update-keys $ csk/->kebab-case-keyword))]
    {:uniprotkb (->> uni-prot-kb-id
                     (mapcat :accessions)
                     (distinct)
                     (mapv api.uniprot/uniprotkb-entry))
     :uniparc   (->> uni-parc
                     (map :memberId)
                     (distinct)
                     (mapv api.uniprot/uniparc-entry))}))

#_(def b
    (edn/read (java.io.PushbackReader. (io/reader "blast-mrcb.edn"))))

#_(def mrcb-blast-proteins
    (mapv (comp api.uniprot/uniprotkb-entry :id) b))


;; (->> (concat (:uniparc mrcb-uniref-90-entries)
;;              (:uniprotlb mrcb-uniref-90-entries))
;;      (map (comp formats.fasta/->fasta
;;                 #(or (uniprot.core/domain-restricted-protein "transpeptidase" %) %)))
;;      #_(clustalo/clustalo))


#_(map (partial uniprot.core/domains "transpeptidase") (:uniprotkb mrcb-uniref-90-entries))


;; --- Interessante Sachen ---

;; Tabellenansicht der Features auf den Uniparc-Dingern
;; (->> (:uniparc mrcb-uniref-90-entries)
;;      (mapv (comp #(some-> % (with-meta {:portal.viewer/default :portal.viewer/table}))
;;                  :sequenceFeatures)))

;; das hier ist der Grund warum ich bei domain-restriction nur nach INTERPRO groups suche
;; (filter (complement :interproGroup)
;;         (mapcat :sequenceFeatures
;;          (:uniparc mrcb-uniref-90-entries)))


;; das hier zeigt, dass was der Uniprot-BLAST ausspuckt nicht zu den queries
;; korrespondiert die man für die entries in KB findet
;; (->> (map
;;       (fn [a b]
;;         (with-meta [a b] {:portal.viewer/default :portal.viewer/diff-text}))
;;       (mapv (comp :value :sequence) mrcb-blast-proteins)
;;       (map (comp :query-seq first :alignments) b))
;;      (filter (fn [[a b]]
;;                (not= a b))))


;; doing blast


;; (def mrcB "MTRPRSPRSRNSKARPAPGLNKWLGWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKQRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQVEQRFDSGAVYLVQNAMQRVMREGTGRSVYSQLPSSLTLAGKTGTSNDSRDSWFSGFGGDLQAVVWLGRDDNGKTPLTGATGALQVWASFMRKAHPQSLEMPMPENVVMAWVDAQTGQGSAADCPNAVQMPYIRGSEPAQGPGCGSQNPAGEVMDWVRGWLN")

;; (defonce bonkers
;;   (blast/run-blast-query!
;;    {:blast/database       :uniprot-bacteria
;;     :blast/query-sequence mrcB}))

;; @bonkers

;; (with-open [wr (clojure.java.io/writer "blast-mrcb.edn")]
;;   (.write wr (with-out-str (clojure.pprint/pprint @bonkers))))

(cypher/merge-graph!
 graph.accrete.core/connection
 (mapping.uniprot.blast/blast-result->blast-graph
  pa-mrcb
  {:blast/database       :uniprot-bacteria
   :blast/query-sequence mrcB}
  b))


(cypher/create-merge-graph-query
 (mapping.uniprot.blast/blast-result->blast-graph
  pa-mrcb
  {:blast/database       :uniprot-bacteria
   :blast/query-sequence mrcB}
  b))


