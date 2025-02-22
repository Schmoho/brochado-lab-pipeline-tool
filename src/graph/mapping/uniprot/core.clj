(ns graph.mapping.uniprot.core
  (:require [graph.mapping.utils :refer :all]
            [graph.mapping.uniprot.ncbi :as ncbi]
            [clojure.string :as str]
            [clojure.set :as set]))

(defn taxon-ref-id [taxon] (sanitize-ref-id (str "T_UNIPROT_" (:taxonId taxon))))
(defn proteome-ref-id [proteome] (sanitize-ref-id (str "PP_UNIPROT_" (:id proteome))))
(defn protein-ref-id [protein] (sanitize-ref-id (str "P_UNIPROT_" (:primaryAccession protein))))
(defn evidence-ref-id [evidence] (sanitize-ref-id (str "E_UNIPROT_" (:source evidence) "_" (:id evidence))))

;; taxons
(defn taxon->taxon-node
  [taxon]
  {:ref-id (taxon-ref-id taxon)
   :labels [:taxon :uniprot-taxon]
   :props  {:id              (:taxonId taxon)
            :scientific-name (:scientificName taxon)
            :rank            (:rank taxon)
            :proteome-count  (get-in taxon [:statistics :proteomeCount])}})

(defn taxon->taxon-ancestry
  [taxon]
  (let [lineage-nodes      (->> (set (map taxon->taxon-node (:lineage taxon))))
        ancestry-relations (->> taxon
                                (conj (:lineage taxon))
                                (partition 2 1)
                                (map
                                 (fn [[parent child]]
                                   (let [parent-id (:taxonId parent)
                                         child-id  (:taxonId child)]
                                     {:type   :lineage-parent
                                      :ref-id (rel-ref-id
                                               (taxon-ref-id parent)
                                               (taxon-ref-id child))
                                      :from   {:ref-id (taxon-ref-id parent)
                                               :labels [:taxon :uniprot-taxon]
                                               :props  {:id parent-id}}
                                      :to     {:ref-id (taxon-ref-id child)
                                               :labels [:taxon :uniprot-taxon]
                                               :props  {:id child-id}}})))
                                set)
        database-relations (->> lineage-nodes
                                (map (partial
                                      rel-between
                                      :contains
                                      {:ref-id "D"
                                       :labels [:database]
                                       :props  {:id "UniprotTaxonomy"}}))
                                set)]
    {:lookups #{{:ref-id "D"
                :labels [:database]
                :props  {:id "UniprotTaxonomy"}}}
     :nodes   lineage-nodes
     :rels    (set/union ancestry-relations database-relations)}))

(defn taxon->neo4j
  [taxon]
  (let [t-node   (taxon->taxon-node taxon)
        entities (merge-with
                  into
                  {:lookups #{{:ref-id "D"
                              :labels [:database]
                              :props  {:id "UniprotTaxonomy"}}}
                   :nodes   [t-node]
                   :rels    [(rel-between
                            :contains
                            {:ref-id "D"
                             :labels [:database]
                             :props  {:id "UniprotTaxonomy"}}
                            t-node)]}
                  (taxon->taxon-ancestry taxon))
        ref-ids  (->> entities
                      vals
                      (apply concat)
                      (map :ref-id)
                      distinct)]
    (-> (assoc entities :returns ref-ids)
        (sanitize-graph))))

;; proteomes
(defn proteome->proteome-node
  [proteome]
  {:ref-id (proteome-ref-id proteome)
   :labels [:proteome :uniprot-proteome]
   :props  (merge
            (select-keys proteome [:id
                                   :proteinCount
                                   :geneCount
                                   :strain
                                   :modified
                                   :proteomeType
                                   :redundantTo])
            (:proteomeStatistics proteome))})

(defn proteome->taxon-relation
  [proteome]
  (let [taxon       (-> proteome :taxonomy)
        tax-id      (-> taxon :taxonId)
        proteome-id (-> proteome :id)]
    {:type   :has-proteome
     :ref-id (rel-ref-id
              (taxon-ref-id taxon)
              (proteome-ref-id proteome))
     :from   {:ref-id (taxon-ref-id taxon)
              :labels [:taxon :uniprot-taxon]
              :props  {:id tax-id}}
     :to     {:ref-id (proteome-ref-id proteome)
              :labels [:proteome :uniprot-proteome]
              :props  {:id proteome-id}}}))

(defn proteome->neo4j
  [proteome]
  (let [assembly-rel (ncbi/proteome->assembly-relation proteome)
        p-node       (proteome->proteome-node proteome)
        entities     {:lookups #{{:ref-id "D"
                                  :labels [:database]
                                  :props  {:id "Proteomes"}}}
                      :nodes   [p-node]
                      :rels    [(proteome->taxon-relation proteome)
                              (rel-between
                               :contains
                               {:ref-id "D"
                                :labels [:database]
                                :props  {:id "Proteomes"}}
                               p-node)
                              assembly-rel]}
        ref-ids      (->> entities
                          vals
                          (apply concat)
                          (map :ref-id)
                          set)]
    (->> (assoc entities :returns ref-ids)
         (sanitize-graph))))

;; proteins
(defn protein->protein-node
  [protein]
  {:ref-id (protein-ref-id protein)
   :labels [:protein :uniprot-protein]
   :props  (merge
            (select-keys
             protein
             [:annotationScore
              :proteinExistence
              :entryType
              :uniProtkbId])
            {:id                     (:primaryAccession protein)
             :sequence               (-> protein :sequence :value)
             :sequence-weight        (-> protein :sequence :molWeight)
             :recommended-name       (some->
                                      protein
                                      :proteinDescription
                                      :recommendedName
                                      :fullName
                                      :value)
             :recommended-short-name (some->
                                      protein
                                      :proteinDescription
                                      :recommendedName
                                      :shortName
                                      :value)})})

(defn feature->feature-node
  [feature ref-id]
  {:ref-id ref-id
   :labels [:protein-feature :uniprot-protein-feature]
   :props  {:id             ref-id
            :type           (:type feature)
            :start          (-> feature :location :start :value)
            :start-modifier (-> feature :location :start :modifier)
            :end            (-> feature :location :end :value)
            :end-modifier   (-> feature :location :end :modifier)
            :description    (feature :description)}})

;; ligands in features reference ChEBI,
;; which is not contained in the list of Uniprot reference databases
;; and which only exposes a WSDL webservice
#_:featureCrossReferences
#_[{:database "ChEBI", :id "CHEBI:58805"}],

(defn protein->neo4j
  [protein]
  (let [p-node   (protein->protein-node protein)
        f-nodes  (->> (map-indexed
                       (fn [idx feature]
                         (feature->feature-node
                          feature
                          (str "PF_" (:primaryAccession protein) "_" idx)))
                       (:features protein)))
        f-rels   (->> f-nodes
                      (map (partial rel-between
                                    :has-feature
                                    p-node)))
        entities {:lookups [{:ref-id "D"
                             :labels [:database]
                             :props  {:id "UniprotKB"}}]
                  :nodes   (concat [p-node]
                                   f-nodes)
                  :rels    (conj f-rels
                                 (rel-between
                                  :contains
                                  {:ref-id "D"
                                   :labels [:database]
                                   :props  {:id "UniprotKB"}}
                                  p-node))}
        ref-ids  (->> entities
                      vals
                      (apply concat)
                      (map (comp :ref-id))
                      set)
        result   (->> (assoc entities :returns ref-ids)
                      (sanitize-graph))]
    result))



#_(let [protein user/uniprot-protein]
    (protein->neo4j protein))

;; evidences should reference ECO terms
;; those need to be imported first
#_(defn evidence->evidence-node
    [evidence]
    {:ref-id (evidence-ref-id evidence)
     :labels [:evidence :uniprot]
     :props  {:evidenceCode (:evidenceCode evidence)
              :source       (:source evidence)
              :id           (:id evidence)}})


#_(->> user/uniprot-proteome-proteins
       (map :uniProtKBCrossReferences)
       (apply concat))

;; databases
(defn database->database-node
  [database]
  (->
   {:ref-id (sanitize-ref-id (:abbrev database))
    :labels [:database]
    :props  {:id                             (:abbrev database)
             :uniprot-id                     (:id database)
             :category                       (:category database)
             :name                           (:name database)
             :doi                            (:doiId database)
             :uniprot-reviewedProteinCount   (-> database :statistics :reviewedProteinCount)
             :uniprot-unreviewedProteinCount (-> database :statistics :unreviewedProteinCount)
             :urls                           (doall (:servers database))
             :template-url                   (:dbUrl database)
             :pubmed-id                      (:pubMedId database)
             :uniprot-linkType               (:linkType database)}}
   (update :props sanitize)))


(defn protein->cross-ids
  [protein]
  (let [protein-id       (:primaryAccession protein)
        cross-references (:uniProtKBCrossReferences protein)]
    (-> (group-by :database cross-references)
        (update-vals (partial map :id))
        (update-keys
         {"KEGG"      :kegg
          "Proteomes" :uniprot/proteomes})
        (assoc :uniprot/taxonomy [(-> protein :organism :taxonId)])
        (dissoc nil))))

#{#_"RefSeq"
  "PDB"
  "AlphaFoldDB"
  "STRING"
  "KEGG"
  "PseudoCAP"
  #_"OrthoDB"
  "BioCyc"
  "Proteomes"
  "GO"
  #_"InterPro"
  #_"PANTHER"}
