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
   [clojure.set :as set]
   [formats.fasta :as formats.fasta]
   [clojure.tools.logging :as log]))

;; benchmark den Uniprot Blast damit man abschätzen kann wie lange der braucht
;; führ das alles auf separaten threads aus
;; implementier die restriction
;; factor die restriction raus, s.d. man das separat ausführen kann

(def running-blast-jobs (atom #{}))

(defn pipeline
  [{:keys [params.uniprot/blast
           params.uniprot/uniref
           params.uniprot/protein
           params.uniprot/taxonomy
           pipeline/uuid]}]
  (.mkdirs (io/file (format "results/%s" uuid)))
  (let [protein-ids    (set (:protein-ids protein))
        gene-names     (set (:gene-names protein))
        taxonomy-level (or (:top-level taxonomy))]
    (doseq [protein-id protein-ids]
      (let [protein    (-> protein-id (api.uniprot/uniprotkb-entry))
            blast-dummy (future (swap! running-blast-jobs conj uuid)
                                (Thread/sleep (* 90 1000))
                                (swap! running-blast-jobs disj uuid))
            #_#_taxon      (-> protein :organism :taxonId (api.uniprot/taxonomy-entry))
            ;; TODO blast muss asynchron ausgeführt werden!
            ;; blast                  (blast/run-blast-query!
            ;;                         {:blast/database database
            ;;                          :blast/query-sequence (-> protein :sequence :value)})
            ;; blast-proteins         (->> @blast (mapv (comp api.uniprot/uniprotkb-entry :id)))
            gene-names             (or gene-names
                                       (->> protein :genes (map (comp :value :geneName))))
            proteins-by-gene-names (if (not-empty gene-names)
                                     (api.uniprot/proteins-by-taxa-and-genes
                                      [(-> protein :organism :scientificName)]
                                      gene-names)
                                     [])
            cluster-type->proteins (api.uniprot/uniref-proteins-by-protein-id
                                    protein-id
                                    (:cluster-types uniref))]
        (tap> proteins-by-gene-names)
        (tap> cluster-type->proteins)
        (if (< 1 (count proteins-by-gene-names))
          (do
            (log/info "Align by taxonomy")
            (->> proteins-by-gene-names
                 (map formats.fasta/->fasta)
                 (clustalo/clustalo)
                 (formats.fasta/->fasta-string :clustalo/aligned-sequence)
                 (spit (format "results/%s/%s_%s_taxonomy.fa" uuid protein-id (str/join "-" gene-names)) )))
          (log/info "Only one protein in taxonomy, nothing to align."))
        (doseq [[cluster-type {:keys [uniprotkb uniparc]}]
                cluster-type->proteins]
          (->> (concat uniprotkb uniparc)
               (map formats.fasta/->fasta)
               (clustalo/clustalo)
               (formats.fasta/->fasta-string :clustalo/aligned-sequence)
               (spit (format "results/%s/%s_%s_%s.fa" uuid protein-id (str/join "-" gene-names) cluster-type))))
        (log/info "Done.")))))


#_ (do
     (def protein-id "A0A0H2ZHP9")
     (def protein (-> protein-id (api.uniprot/uniprotkb-entry)))

     (def taxon
       (-> protein :organism :taxonId (api.uniprot/taxonomy-entry)))

     (defn upstream-species
       [taxon]
       (->> taxon
            :lineage
            (drop-while #(not= (:rank %) "species"))
            first))

     (def taxon-lineage
       (->> (upstream-species taxon)
            :taxonId
            (api.uniprot/downstream-lineage)))

     (def all-taxon-proteins-by-gene-name
       (api.uniprot/uniprotkb-search {:query (format "gene:%s AND taxonomy_name:%s"
                                                     "mrcb" "Pseudomonas aeruginosa")}))

     (def uniref-cluster
       (api.uniprot/uniref-by-protein-id (:primaryAccession protein)))

     (def uniref-90
       (api.uniprot/uniref-entry #_"UniRef50_Q4K603"
                                 "UniRef90_G3XD31"
                                 #_"UniRef100_A0A0U4NUB5"))

     (def uniref-90-entries
       (let [{:keys [uni-prot-kb-id uni-parc]}
             (as-> uniref-90 $
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


     (def blast (edn/read (java.io.PushbackReader. (io/reader "blast-mrcb.edn"))))
     (def blast-proteins (->> blast (mapv (comp api.uniprot/uniprotkb-entry :id)))))


;; (->> mrcb-uniref-90-entries
;;      (map (comp formats.fasta/->fasta
;;                 #_(or (uniprot.core/domain-restricted-protein "transpeptidase" %) %)))
;;      (clustalo/clustalo))


;; (with-open [wr (clojure.java.io/writer "uniref-50.edn")]
;;   (.write wr (with-out-str (clojure.pprint/pprint (->> (concat (:uniprotkb mrcb-uniref-90-entries)
;;                                                               (:uniparc mrcb-uniref-90-entries))
;;                                                        (mapv formats.fasta/->fasta))))))


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


;; ungefähr so sollte das nachher abgespeichert werden
;; {"path"
;;  {"/raw-data"
;;   {"/uniprot"
;;    ["proteins" "taxons" "cluster" "blast"]}
;;   "/alignments"
;;   {"/blast"
;;    ["/{domain}" "full"]
;;    "/taxonomy"
;;    {"/{gene}"
;;     ["/{domain}" "full"]}
;;    "/uniref90"
;;    ["/{domain}" "full"]
;;    "/uniref100"
;;    ["/{domain}" "full"]}}}


;; doing blast

;; (let [before (atom nil)
;;       after (atom nil)]
;;   (reset! before (java.time.Instant/now))
;;   (Thread/sleep 5000)
;;   (reset! after (java.time.Instant/now))
;;   [@before @after])


;; (def mrcB "MTRPRSPRSRNSKARPAPGLNKWLGWALKLGLVGLVLLAGFAIYLDAVVQEKFSGRRWTIPAKVYARPLELFNGLKLSREDFLRELDALGYRREPSVSGPGTVSVAASAVELNTRGFQFYEGAEPAQRVRVRFNGNYVSGLSQANGKELAVARLEPLLIGGLYPAHHEDRILVKLDQVPTYLIDTLVAVEDRDFWNHHGVSLKSVARAVWVNTTAGQLRQGGSTLTQQLVKNFFLSNERSLSRKINEAMMAVLLELHYDKRDILESYLNEVFLGQDGQRAIHGFGLASQYFFSQPLAELKLDQVALLVGMVKGPSYFNPRRYPDRALARRNLVLDVLAEQGVATQQEVDAAKQRPLGVTRQGSMADSSYPAFLDLVKRQLRQDYRDEDLTEEGLRIFTSFDPILQEKAETSVNETLKRLSGRKGVDQVEAAMVVTNPETGEIQALIGSRDPRFAGFNRALDAVRPIGSLIKPAVYLTALERPSKYTLTTWVQDEPFAVKGQDGQVWRPQNYDRRSHGTIFLYQGLANSYNLSTAKLGLDVGVPNVLQTVARLGINRDWPAYPSMLLGAGSLSPMEVATMYQTIASGGFNTPLRGIRSVLTADGQPLKRYPFQVEQRFDSGAVYLVQNAMQRVMREGTGRSVYSQLPSSLTLAGKTGTSNDSRDSWFSGFGGDLQAVVWLGRDDNGKTPLTGATGALQVWASFMRKAHPQSLEMPMPENVVMAWVDAQTGQGSAADCPNAVQMPYIRGSEPAQGPGCGSQNPAGEVMDWVRGWLN")

;; (defonce bonkers
;;   (blast/run-blast-query!
;;    {:blast/database       :uniprot-bacteria
;;     :blast/query-sequence mrcB}))

;; @bonkers

;; (with-open [wr (clojure.java.io/writer "blast-mrcb.edn")]
;;   (.write wr (with-out-str (clojure.pprint/pprint @bonkers))))


