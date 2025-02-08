(ns workbench.structure-comparison
  (:require
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [comparison :as comparison]
   [dk.ative.docjure.spreadsheet :as doc]
   [docking.afdb :refer :all]
   [docking.core :refer :all]
   [pdb :as pdb]
   [uniprot.api]
   [utils :refer :all]
   [workbench.datasets :as datasets]
   [clojure.math :as math]))

;; (def aligned-ecoli-unrelaxed
;;   (comparison/align-3d-structure!
;;    "tax/BW25113/P02919.pdb"
;;    "resources/md_cef/ec/ec_main.pdb"
;;    "resources/md_cef/ec"
;;    "aligned_unrelaxed_ec"))

;; (def aligned-pseudomonas-unrelaxed
;;   (comparison/align-3d-structure!
;;    "tax/PA14/A0A0H2ZHP9.pdb"
;;    "resources/md_cef/pa/pa_main.pdb"
;;    "resources/md_cef/pa"
;;    "aligned_unrelaxed_pa"))

;; (def aligned-salmonella-unrelaxed
;;   (comparison/align-3d-structure!
;;    "tax/14028s/A0A0F6AWZ0.pdb"
;;    "resources/md_cef/st/st_main.pdb"
;;    "resources/md_cef/st"
;;    "aligned_unrelaxed_st"))

;; (range 66 167) Pseudomonas
;; (range 103 205) Salmonella

;; (let [protein              datasets/pbp-1b-st
;;       active-site-residues (let [active-site (-> (uniprot.core/transpeptidase-sites protein) first :location :start :value)
;;                                  distance    9]
;;                              (->> (set/union (set (pdb/residue-numbers-in-distance active-site
;;                                                                                    distance
;;                                                                                    (get-in protein [:structure :relaxed])))
;;                                              (set (pdb/residue-numbers-in-distance active-site
;;                                                                                    distance
;;                                                                                    (get-in protein [:structure :unrelaxed]))))
;;                                   vec sort))]
;;   (spit
;;    "salmonella-data.json"
;;   (json/generate-string
;;    {:title                   "PBP1b in Salmonella"
;;     :backbone-distances      (json/parse-string (slurp "resources/md_cef/st/aligned_unrelaxed_st-backbone-distances.json"))
;;     :highlights-in-full-plot [{:residues (range 103 205)
;;                                :color    "gold"
;;                                :label    "UB2H Domain"}
;;                               {:residues active-site-residues
;;                                :color    "mediumpurple"
;;                                :label    "Transpeptidase Active Site"}]
;;     :subplots                [{:residues (range 103 205)
;;                                :color    "gold"
;;                                :title    "UB2H Domain"}
;;                               {:residues active-site-residues
;;                                :color    "mediumpurple"
;;                                :title    "Transpeptidase Active Site"}]})))

;; (let [protein              datasets/pbp-1b-st
;;       active-site          (-> (uniprot.core/transpeptidase-sites protein) first :location :start :value)
;;       active-site-residues (let [distance 9]
;;                              (->> (set/union (set (pdb/residue-numbers-in-distance active-site
;;                                                                                    distance
;;                                                                                    (get-in protein [:structure :relaxed])))
;;                                              (set (pdb/residue-numbers-in-distance active-site
;;                                                                                    distance
;;                                                                                    (get-in protein [:structure :unrelaxed]))))
;;                                   vec sort))]
;;   active-site)

(def backbone-names #{"N" "HN" "H1" "H2" "H3" "CA" "HA" "C" "O" "OT2" "OT1"})

(defn atoms-by-residue->side-chain-atoms-by-residue
  [[n ats]]
  (when n
    [n (->> ats
            (filter (comp (complement backbone-names) :name))
            (sort-by :residue-sequence-number ))]))

(defn rmsd
  [atoms1 atoms2]
  (let [distances (map pdb/distance atoms1 atoms2)]
    (math/sqrt (/ (reduce + (map #(math/pow % 2) distances))
                  (count distances)))))

;; (let [relaxed                (-> datasets/pbp-1b-pa :structure :relaxed)
;;       unrelaxed              (pdb/parsed-pdb (slurp (format "%s/out/%s-hydrogenated-aligned.pdb"
;;                                                             (datasets/uniprot-path datasets/pseudomonas)
;;                                                             (datasets/pbp-1b datasets/pseudomonas))))
;;       residue-numbers        (map :residue-sequence-number
;;                                   (first (comparison/parallel-common-residues relaxed unrelaxed)))
;;       relaxed-sc-by-number   (->> (group-by :residue-sequence-number relaxed)
;;                                   (map atoms-by-residue->side-chain-atoms-by-residue)
;;                                   (into {}))
;;       unrelaxed-sc-by-number (->> (group-by :residue-sequence-number unrelaxed)
;;                                   (map atoms-by-residue->side-chain-atoms-by-residue)
;;                                   (into {}))
;;       rmsds                  (sort-by first
;;                                       (for [[n ats-relaxed] relaxed-sc-by-number]
;;                                         (let [ats-unrelaxed (get unrelaxed-sc-by-number n)]
;;                                           [n (rmsd ats-unrelaxed ats-relaxed)])))]
;;   (spit "rmsds-pa.json" (json/generate-string rmsds)))






;; => ("N" "H1" "H2" "H3" "CA" "HA" "CB" "HB1" "HB2" "CG" "HG" "CD1" "1HD1" "2HD1" "3HD1" "CD2" "1HD2" "2HD2" "3HD2" "C" "O")
