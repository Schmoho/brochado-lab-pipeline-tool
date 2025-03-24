(ns uniprot
  (:require
   [clojure.string :as str]
   [schmoho.formats.fasta :refer [->fasta]]
   [schmoho.utils.file :as utils]
   [schmoho.biodb.uniprot.core :as uniprot :refer :all]
   [schmoho.biodb.uniprot.mapping :refer :all]
   [schmoho.biodb.kegg.api :as api.kegg]
   [schmoho.biodb.uniprot.api :as api.uniprot]
   [clojure.set :as set]))

(comment
  (require 'user)
  (dois user/uniprot-protein)
  (protein-sequence user/uniprot-protein)
  (active-sites user/uniprot-protein)

  (defn distinct-sequences-count
    [uniprot-proteins]
    (count (distinct (map (comp :value :sequence second) uniprot-proteins))))

  (defn organism-counts
    [uniprot-proteins]
    (let [grouped (group-by :organism uniprot-proteins)]
      (->> grouped
           (map (fn [[k v]]
                  [k (count v)]))
           (into {})
           (sort-by (comp :scientificName first)))))

  (defn bin-by-length
    [strings step-size]
    (let [lengths   (map count strings)
          min-len   (apply min lengths)
          max-len   (apply max lengths)
          bins      (range min-len (+ max-len step-size) step-size)
          bin-label (fn [l]
                      (* step-size (int (Math/floor (/ l step-size)))))]
      (reduce (fn [acc s]
                (let [length (count s)
                      bin    (bin-label length)]
                  (update acc bin (fnil inc 0))))
              (sorted-map)
              strings)))

  (defn sequence-length-overview
    [uniprot-proteins number-of-bins]
    (let [seqs       (keys (group-by protein-sequence uniprot-proteins))
          counts     (map count seqs)
          min-length (apply min counts)
          max-length (apply max counts)]
      {:sequence-length-range     [min-length max-length]
       :sequence-length-histogram (bin-by-length seqs (int (/ (- max-length min-length) number-of-bins)))}))

  (defn count-by
    [accessor uniprot-proteins]
    (->> uniprot-proteins
         (group-by (comp accessor val))
         (map (fn [[k v]] [k (count v)]))
         (into {})))

  (defn transpeptidase-sites
    [record]
    (->> (active-sites record)
         (filter #(str/includes? (:description %) "transpeptidase"))))

  (defn has-transpeptidase-site?
    [uniprot-record]
    (-> uniprot-record
        transpeptidase-sites
        not-empty))

  (defn protein-set-description
    [uniprot-proteins]
    (into (sorted-map)
          [{:total-number                                       (->> uniprot-proteins count)
            :has-transpeptidase-annotation                      (->> uniprot-proteins (filter has-transpeptidase-site?) count)
            :number-of-organisms                                (->> uniprot-proteins (group-by :organism) count)
            :number-of-organisms-with-transpeptidase-annotation (->> uniprot-proteins (filter has-transpeptidase-site?) (group-by :organism) count)
            :represented-lineages                               (->> uniprot-proteins (utils/represented-values (comp last :lineage :organism)))
            :protein-evidence                                   (->> uniprot-proteins (count-by :proteinExistence))
            :entry-type                                         (->> uniprot-proteins (count-by :entryType))
            :distinct-sequences                                 (->> uniprot-proteins distinct-sequences-count)
            :distinct-sequences-with-transpeptidase-annotation  (->> uniprot-proteins (filter has-transpeptidase-site?) distinct-sequences-count)}
           (sequence-length-overview uniprot-proteins 20)]))

  (require 'math)

  (defn go-term-map-stats
    [go-term-map]
    (->> (for [[type terms] go-term-map]
           (let [freqs (vals (frequencies terms))]
             [type {:count            (count terms)
                    :mean-frequency   (int (math/mean freqs))
                    :median-frequency (int (math/median freqs))
                    :max-frequency    (apply max freqs)}]))
         (into {})))

  (defonce uniprot-proteome-ecoli
    (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))

  (defonce uniprot-proteome-pseudo
    (api.uniprot/uniprotkb-stream {:query "taxonomy_id:208963"}))

  (let [proteome-1 uniprot-proteome-ecoli
        proteome-2 uniprot-proteome-pseudo
        go-terms-1 (go-terms-in-proteome proteome-1)
        go-terms-2 (go-terms-in-proteome proteome-2)]
    {:proteome-1 (go-term-map-stats go-terms-1)
     :proteome-2 (go-term-map-stats go-terms-2)}))





(comment

  (do
    (defonce uniprot-proteome-ecoli
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:83333"}))
    (defonce uniprot-proteome-pseudo
      (api.uniprot/uniprotkb-stream {:query "taxonomy_id:208963"}))
    (def kegg-proteome-ecoli
      (utils/read-file "kegg-83333.edn"))
    (def kegg-proteome-pseudo
      (utils/read-file "kegg-208963.edn")))

  (future
    (->> uniprot-proteome-pseudo
         (transduce
          (comp
           (uniprot/database-lookup-xform "KEGG")
           (map second)
           api.kegg/download-kegg-xform)
          conj
          [])
         (apply concat)
         #_(utils/write! "kegg-208963.edn")))

  (orthology-mapping-rel uniprot-proteome-ecoli
                         kegg-proteome-ecoli)

  (uniprot-kegg-id-mapping-rel uniprot-proteome-ecoli
                               kegg-proteome-ecoli
                               uniprot-proteome-pseudo
                               kegg-proteome-pseudo))
