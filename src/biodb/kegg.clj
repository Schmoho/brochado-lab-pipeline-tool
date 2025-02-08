(ns kegg
  (:require
   [kegg.api :as kegg.api]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [clojure.java.io :as java.io]
   [clojure.data.csv :as csv]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.shell :as sh]
   [clojure.tools.logging :as log]
   [excel :as excel]
   [clojure.set :as set]
   [clojure.java.io :as io]))

;; (let [ec-gene-counts (->> ap-ec-amikacin
;;                           (map (fn [[pathway genes]]
;;                                  [pathway {:ec genes}]))
;;                           (into {}))
;;       pa-gene-counts (->> ap-pa-amikacin
;;                           (map (fn [[pathway genes]]
;;                                  [pathway {:pa genes}]))
;;                           (into {}))
;;       dat            (->> (merge-with merge ec-gene-counts pa-gene-counts)
;;                (map
;;                 (fn [[ko-id {:keys [ec pa]}]]
;;                   [ko-id
;;                    (or (:name ec) (:name pa))
;;                    (-> ec :genes count)
;;                    (-> pa :genes count)
;;                    (->> ec :genes (str/join " "))
;;                    (->> pa :genes (str/join " "))])))]
;;   (with-open [writer (io/writer "amikacin-comp.csv")]
;;     (csv/write-csv writer
;;                    (concat [["KO-ID"
;;                              "Pathway Name"
;;                              "EC-Genes Participants"
;;                              "#EC-Genes Participants"
;;                              "#PA-Genes Participants"
;;                              "PA-Genes Participants"]]
;;                            dat))))

(def eco-genes (kegg.api/get-all-genes! "eco"))
(def eco-paths (kegg.api/get-all-pathways! "eco"))
(def eco-gene-orthology-lookup (gene-orthology-lookup eco-genes))
(def eco-uniprot-orthology-lookup (uniprot-orthology-lookup eco-genes))
(def eco-kegg->uniprot (kegg->uniprot-lookup eco-genes))
(def eco-gene-name-lookup (gene-name-lookup eco-genes))
(def eco-gene->pathway (gene->pathway-lookup eco-paths))
(def eco-ap-amikacin
  (affected-pathways eco-gene->pathway
                     eco-gene-name-lookup
                     excel/ec-amikacin-hits
                     eco-paths))
(def eco-ap-cefo
  (affected-pathways eco-gene->pathway
                     eco-gene-name-lookup
                     excel/ec-cefotaxime-hits
                     eco-paths))

(def pau-genes (kegg.api/get-all-genes! "pau"))
(def pau-paths (kegg.api/get-all-pathways! "pau"))
(def pau-gene-orthology-lookup (gene-orthology-lookup pau-genes))
(def pau-uniprot-orthology-lookup (uniprot-orthology-lookup pau-genes))
(def pau-kegg->uniprot (kegg->uniprot-lookup pau-genes))
(def pau-gene-name-lookup (gene-name-lookup pau-genes))
(def pau-gene->pathway (gene->pathway-lookup pau-paths))
(def pau-ap-amikacin
  (affected-pathways pau-gene->pathway
                     pau-gene-name-lookup
                     excel/pa-amikacin-hits
                     pau-paths))
(def pau-ap-cefo
  (affected-pathways pau-gene->pathway
                     pau-gene-name-lookup
                     excel/pa-cefotaxime-hits
                     pau-paths))
(def pau-uniprot->kegg
  (->> pau-genes
      (map (fn [[gene-id gene-data]]
             [(-> gene-data :DBLINKS :UniProt) gene-id]))
      (into {})))



(let [p-m (let [gene->pathway   pau-gene->pathway
                pathway-mapping (into {}
                                      (map (juxt first (comp first :KO_PATHWAY second))
                                           pau-paths))
                refd-pathways   (distinct (flatten (vals gene->pathway)))]
            (map pathway-mapping refd-pathways))
      e-m (let [gene->pathway   eco-gene->pathway
                pathway-mapping (into {}
                                      (map (juxt first (comp first :KO_PATHWAY second))
                                           eco-paths))
                refd-pathways   (distinct (flatten (vals gene->pathway)))]
            (map pathway-mapping refd-pathways))]
  (count (set/intersection (set p-m) (set e-m))))


(->> (affected-genes eco-gene-name-lookup
                     excel/ec-cefotaxime-hits)
     (map (comp (gene-orthology-lookup eco-genes) first)))



;; (let [gene-set (fn [workbook]
;;                  (->> (excel/genes workbook)
;;                       (filter (fn [gene]
;;                                 (str/starts-with? gene "PA14")))
;;                       set))]
;;   (set/difference
;;    (set/union
;;     (gene-set excel/pa-cefotaxime)
;;     (gene-set excel/pa-amikacin))
;;    (set (keys kegg-pseudomonas))))
;; ;; => #{}
