(ns schmoho.dasudopit.pipeline.taxonomy
  (:require
   [clojure.tools.logging :as log]
   [schmoho.biodb.uniprot.api :as api.uniprot]
   [schmoho.biodb.uniprot.core :as uniprot]
   [schmoho.biotools.clustalo :as clustalo]
   [schmoho.formats.fasta :as formats.fasta]
   [schmoho.utils.file :as utils]))

(def running-blast-jobs (atom #{}))

;; metadata!
(defn with-taxonomy-protein-set
  [gene-names]
  (map
   (fn [protein]
     (when-let [gene-names (not-empty
                            (or gene-names
                                (->> protein :genes (map (comp :value :geneName)))))]
       (api.uniprot/proteins-by-taxa-and-genes
        [(-> protein :organism :scientificName)]
        gene-names)))))

;; metadata!
(defn with-uniref-protein-sets
  [uniref-params]
  (mapcat
   (fn [protein]
     (api.uniprot/uniref-proteins-by-protein-id
      (:primaryAccession protein)
      (:cluster-types uniref-params)))))

;; metadata!
(defn with-spin-off-blast-job
  [uuid blast-params]
  (map
   (fn [protein]
     ;; benchmark den Uniprot Blast damit man abschätzen kann wie lange der braucht
     ;; blast                  (blast/run-blast-query!
     ;;                         {:blast/database database
     ;;                          :blast/query-sequence (-> protein :sequence :value)})
     ;; blast-proteins         (->> @blast (mapv (comp api.uniprot/uniprotkb-entry :id)))
     (future (swap! running-blast-jobs conj uuid)
             (Thread/sleep (* 90 1000))
             (swap! running-blast-jobs disj uuid)))))

;; metadata!
(defn domain-restricting
  [restriction-domains]
  (mapcat
   (fn [protein-set]
     (conj
      (for [restriction-domain restriction-domains]
        (map (partial uniprot/domain-restricted-protein restriction-domain)
             protein-set))
      protein-set))))

;; metadata!
(defn taxonomy-filtering
  [taxonomy-params]
  (mapcat
   (fn [protein-set]
     [protein-set nil])))

(def align-protein-set
  (comp
    clustalo/clustalo
    (partial map formats.fasta/->fasta)))

(def params
  {:params.uniprot/blast    {:use-blast?                       true
                             :database                         :uniprot-bacteria
                             :filter-blast-result-by-taxonomy? false}
   :params.uniprot/uniref   {:use-uniref?                  true
                             :cluster-types                ["UniRef50" "UniRef90"]
                             :filter-clusters-by-taxonomy? false}
   :params.uniprot/taxonomy {:top-level                            "species"
                             :filter-taxon                         "Pseudomonas"
                             :use-taxonomic-search?                true
                             :really-use-broad-taxonomic-category? false}
   :params.uniprot/protein  {:protein-ids         ["P02919"
                                                   "A0A0H2ZHP9"]
                             :gene-names          ["mrcB"]
                             :restriction-domains ["transpeptidase"
                                                   {"A0A0H2ZHP9" "transglycosylase"}]}})

(defn pipeline
  [{{:keys [use-blast?]
     :as   blast-params}          :params.uniprot/blast
    {:keys [use-uniref?]
     :as   uniref-params}         :params.uniprot/uniref
    {:keys [gene-names
            protein-ids
            restriction-domains]} :params.uniprot/protein
    {:keys [use-taxonomy-search?]
     :as   taxonomy-params}       :params.uniprot/taxonomy
    uuid                          :pipeline/uuid}]
  (let [protein-ids (set protein-ids)
        gene-names  (set gene-names)
        alignments
        (->> protein-ids
             (transduce
              (comp
               (map api.uniprot/uniprotkb-entry)
               (utils/branching
                (when use-taxonomy-search?
                  (with-taxonomy-protein-set gene-names))
                (when use-uniref?
                  (with-uniref-protein-sets uniref-params))
                (when use-blast?
                  (with-spin-off-blast-job uuid blast-params)))
               (domain-restricting restriction-domains)
               (taxonomy-filtering taxonomy-params)
               (map align-protein-set))
              conj
              []))]
    ;;  (< 1 (count proteins-by-gene-names))
    #_(doseq [alignment alignments]
        (->> alignment
             (formats.fasta/->fasta-string :clustalo/aligned-sequence)
             (spit "lol")))
    (log/info "Done.")))




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


