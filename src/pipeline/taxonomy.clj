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
   [biodb.uniprot.blast :as blast]
   #_[progrock.core :as pr]
   [clojure.set :as set]))

;; protein-id
;; ;; -> taxon-id
;; ;; ;; -> lineage
;; ;; ;; ;; -> species-level
;; ;; ;; ;; ;; -> ancestor:species-id
;; ;; ;; ;; ;; ;; -> page through
;; ;; ;; ;; ;; ;; ;; -> gene:geneName + taxonomy
;; ;; -> uniref-clusters
;; ;; ;; -> filter by taxonomic lineage

(def insane-taxonomic-levels
  #{"no rank"
    "phylum"
    "class"
    "order"
    "family"
    "superkingdom"
    "kingdom"
    "clade"
    "subkingdom"
    "subphylum"
    "subclass"
    "suborder"
    "subfamily"
    "tribe"})

(def all-taxonomic-levels
  (set/union #{"species" "strain"}
             insane-taxonomic-levels))

(def protein-id "A0A0H2ZHP9")
(def protein (-> protein-id (api.uniprot/uniprotkb-entry)))

(def blast (edn/read (java.io.PushbackReader. (io/reader "blast-mrcb.edn"))))
(def blast-proteins (->> blast (mapv (comp api.uniprot/uniprotkb-entry :id))))

;; (let [before (atom nil)
;;       after (atom nil)]
;;   (reset! before (java.time.Instant/now))
;;   (Thread/sleep 5000)
;;   (reset! after (java.time.Instant/now))
;;   [@before @after])

;; benchmark den Uniprot Blast damit man abschätzen kann wie lange der braucht
;; führ das alles auf separaten threads aus
;; schreib die alignments raus
;; implementier die restriction
;; factor die restriction raus, s.d. man das separat ausführen kann
;; CLI interface for now?

(defn pipeline
  ([protein-id] (pipeline protein-id {:uniprot.blast/database                         :uniprot-bacteria
                                      :uniprot.blast/filter-blast-result-by-taxonomy? false
                                      :uniprot.uniref/cluster-types                   ["UniRef90" "UniRef100"]
                                      :uniprot.uniref/filter-clusters-by-taxonomy?    false
                                      :uniprot.taxonomy/top-level-taxon               :species}))
  ([protein-id {:keys [gene-names
                       restriction-domains
                       uniprot-blast/database
                       uniprot.taxonomy/top-level-taxon]}]   
   (let [protein (-> protein-id (api.uniprot/uniprotkb-entry))
         taxon   (-> protein :organism :taxonId (api.uniprot/taxonomy-entry))
         taxon-rank (:rank taxon)]
     
     (let [;; TODO blast muss asynchron ausgeführt werden!
           ;; blast                  (blast/run-blast-query!
           ;;                         {:blast/database database
           ;;                          :blast/query-sequence (-> protein :sequence :value)})
           ;; blast-proteins         (->> @blast (mapv (comp api.uniprot/uniprotkb-entry :id)))
           gene-names             (or gene-names (->> protein :genes (map (comp :value :geneName))))
           species                (or (->> pa :lineage (drop-while #(not= (:rank %) "species")) first)
                                      taxon)
           ;; TODO: multiple gene names!
           proteins-by-gene-names (api.uniprot/proteins-by-taxa-and-genes
                                   [(:scientificName species)]
                                   genes)
           cluster-type->proteins (uniref-proteins-by-protein-id protein-id)]
       #_(->> blast-proteins
              (map (comp formats.fasta/->fasta
                         #(or (uniprot.core/domain-restricted-protein "transpeptidase" %) %)))
              (clustalo/clustalo)))
     )))

;; ungefähr so sollte das nachher abgespeichert werden
{"path"
 {"/raw-data"
  {"/uniprot"
   ["proteins" "taxons" "cluster" "blast"]}
  "/alignments"
  {"/blast"
   ["/{domain}" "full"]
   "/taxonomy"
   {"/{gene}"
    ["/{domain}" "full"]}
   "/uniref90"
   ["/{domain}" "full"]
   "/uniref100"
   ["/{domain}" "full"]}}}

(def pa-mrcb
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
#_(->> (:uniparc mrcb-uniref-90-entries)
     (mapv (comp #(some-> % (with-meta {:portal.viewer/default :portal.viewer/table}))
                 :sequenceFeatures)))

;; das hier ist der Grund warum ich bei domain-restriction nur nach INTERPRO groups suche
#_(filter (complement :interproGroup)
        (mapcat :sequenceFeatures
         (:uniparc mrcb-uniref-90-entries)))


;; das hier zeigt, dass was der Uniprot-BLAST ausspuckt nicht zu den queries
;; korrespondiert die man für die entries in KB findet
;; (->> (map
;;       (fn [a b]
;;         (with-meta [a b] {:portal.viewer/default :portal.viewer/diff-text}))
;;       mrcb-blast-proteins
;;       b)
;;      (filter (fn [[a b]] 
;;                (not= (-> a :sequence :value )
;;                      (-> b :alignments first :query-seq)))))


;; doing blast


;; (def mrcB "MTRPRSPRSRNSKARPAPGLNKWLGWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKQRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQVEQRFDSGAVYLVQNAMQRVMREGTGRSVYSQLPSSLTLAGKTGTSNDSRDSWFSGFGGDLQAVVWLGRDDNGKTPLTGATGALQVWASFMRKAHPQSLEMPMPENVVMAWVDAQTGQGSAADCPNAVQMPYIRGSEPAQGPGCGSQNPAGEVMDWVRGWLN")

;; (defonce bonkers
;;   (blast/run-blast-query!
;;    {:blast/database       :uniprot-bacteria
;;     :blast/query-sequence mrcB}))

;; @bonkers

;; (with-open [wr (clojure.java.io/writer "blast-mrcb.edn")]
;;   (.write wr (with-out-str (clojure.pprint/pprint @bonkers))))

;; (cypher/merge-graph!
;;  graph.accrete.core/connection
;;  q(mapping.uniprot.blast/blast-result->blast-graph
;;   pa-mrcb
;;   {:blast/database       :uniprot-bacteria
;;    :blast/query-sequence mrcB}
;;   b))


;; (cypher/create-merge-graph-query
;;  (mapping.uniprot.blast/blast-result->blast-graph
;;   pa-mrcb
;;   {:blast/database       :uniprot-bacteria
;;    :blast/query-sequence mrcB}
;;   b))


;; (def bar (pr/progress-bar 100))

;; (pr/render (pr/tick bar 25))

;; (pr/print (pr/tick bar 35))

;; (loop [bar (pr/progress-bar 100)]
;;   (if (= (:progress bar) (:total bar))
;;     (pr/print (pr/done bar))
;;     (do (Thread/sleep 100)
;;         (pr/print bar)
;;         (recur (pr/tick bar)))))
