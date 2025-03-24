(ns schmoho.dasudopit.excel
  (:require
   [cheshire.core :as json]
   [clojure.math :as math]
   [dk.ative.docjure.spreadsheet :as doc]
   [schmoho.dasudopit.math :refer :all]))

(defn row-count
  [sheet]
  (->> sheet (doc/row-seq) count))

(defn header
  [sheet]
  (->>  sheet
        (doc/row-seq)
        first
        (doc/cell-seq)
        (map #(.getStringCellValue %))))

(defn workbook-description
  [workbook]
  (->> workbook
       (doc/sheet-seq)
       (map (fn [sheet]
              [(doc/sheet-name sheet)
               {:columns (header sheet)
                :row-count (row-count sheet)}]))
       (into {})))

(defn genes
  [workbook]
  (->> workbook
       (doc/select-sheet "fdr_tab")
       (doc/select-columns {:A :gene})
       ;; stellt sich raus, das hier ist leer:
       #_(filter #(not= (:representative %) (:cluster %)))
       (drop 1)
       (map :gene)))

(defn effect-size
  [slope RSS0 RSS1]
  (* (if (pos? slope) 1 -1)
     (math/sqrt (- RSS0 RSS1))))

(defn log-transformed-F-statistic
  [F-statistic]
  (log2 (+ F-statistic 1)))

(defn volcano
  [workbook]
  (->> workbook
       (doc/select-sheet "fdr_tab")
       (doc/select-columns {:A  :gene
                            :M  :effect
                            :P  :F-statistic
                            :H  :RSS0
                            :I  :RSS1
                            :AD :FDR
                            :K  :slope})
       (drop 1)
       (map (fn [{:keys [slope RSS0 RSS1 F-statistic] :as row}]
              (-> row
                  (assoc :effect-size (effect-size slope RSS0 RSS1))
                  (assoc :log-transformed-F-statistic (log-transformed-F-statistic F-statistic)))))))

(defn hits
  [volcano]
  (->> volcano
       (filter #(< (:FDR %) 0.02))
       (filter #(not= (:FDR %) 0.0))
       (sort-by :FDR)))






(comment

  (defonce raw (doc/load-workbook "resources/tpp-raw-cefotaxime-ecoli.xlsx"))
  (defonce processed (doc/load-workbook "resources/tpp-processed-cefotaxime-ecoli.xlsx"))

  (workbook-description raw)

  (def ec-amikacin (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Ecoli.xlsx"))
  (def ec-cefotaxime (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Ecoli.xlsx"))
  (def ec-amikacin-hits
    (->> ec-amikacin
         (volcano)
         (hits)
         (vec)))

  (def ec-cefotaxime-hits
    (->> ec-cefotaxime
         (volcano)
         (hits)
         (vec)))

  (def pa-amikacin (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Paeruginosa.xlsx"))
  (def pa-amikacin-hits
    (->> pa-amikacin
         (volcano)
         (hits)
         (vec)))

  (def pa-cefotaxime (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Paeruginosa.xlsx"))
  (def pa-cefotaxime-hits
    (->> pa-cefotaxime
         (volcano)
         (hits)
         (vec)))

  (def temperature-rank
    {42.0 0,
     45.4 1,
     49.0 2,
     54.8 4,
     67.0 8,
     51.9 3,
     60.5 6,
     57.9 5,
     71.3 9,
     63.6 7})

  (->> raw
       (doc/select-sheet "pEC50")
       (doc/select-columns {:A :gene-name
                            :B :uniprot-id
                            :D :temperature
                            :O :fc-0
                            :P :fc-2.5
                            :Q :fc-10
                            :R :fc-50
                            :S :fc-250})
       (drop 1)
       (filter (complement
                #(or (str/includes? (:uniprot-id %) "CONTAMINANT")
                     (str/includes? (:uniprot-id %) "REV"))))
       (map (fn [row]
              (-> row
                  (update :gene-name #(str/replace % "#" ""))
                  (update :uniprot-id #(str/replace % "#" "")))))
       (filter #(#{0 1} (temperature-rank (:temperature %))))
       (group-by :gene-name)
       (map (fn [[gene-name [temp1 temp2]]]
              [gene-name
               (->>
                (concat
                 (vals (select-keys temp1
                                    [:fc-2.5
                                     :fc-10
                                     :fc-50
                                     :fc-250]))
                 (vals (select-keys temp2
                                    [:fc-2.5
                                     :fc-10
                                     :fc-50
                                     :fc-250])))
                (map log2)
                (sort)
                (median))]))
       (map second)
       ((juxt (partial apply min)
              (partial apply max))))

  (defn write-volcano-plot-data!
    [workbook output-filename]
    (->> (volcano workbook)
         (json/generate-string)
         (spit output-filename)))

  (defn write-hits!
    [workbook output-filename]
    (->> (volcano workbook)
         (hits)
         (map :gene)
         (vec)
         (json/generate-string)
         (spit output-filename)))

  (write-volcano-plot-data! pa-amikacin "pa-amikacin.json")
  (write-volcano-plot-data! pa-cefotaxime "pa-cefotaxime.json")
  (write-volcano-plot-data! ec-amikacin "ec-amikacin.json")
  (write-volcano-plot-data! ec-cefotaxime "ec-cefotaxime.json")
  (write-hits! pa-amikacin "pa-amikacin-hits.json")
  (write-hits! pa-cefotaxime "pa-cefotaxime-hits.json")
  (write-hits! ec-amikacin "ec-amikacin-hits.json")
  (write-hits! ec-cefotaxime "ec-cefotaxime-hits.json")

  (set/difference
   (->> (volcano ec-cefotaxime)
        (sort-by :FDR)
        (filter #(< (:FDR %) 0.01))
        (map :gene)
        set)
   (set hits))

  (->> ec-cefotaxime
       (doc/select-sheet "fdr_tab")
       (doc/select-columns {:A :gene})
     ;; stellt sich raus, das hier ist leer:
       #_(filter #(not= (:representative %) (:cluster %)))
       (drop 1)
       (map :gene)
       (take 3))

  (def ecoli-raw
    (doc/load-workbook "ttp/2020-11-12_results_2D_TPP-cefotaxime-Ecoli.xlsx"))

  (def non-1-to-1-mapped-genes-and-proteins
    (->> ecoli-raw
         (doc/select-sheet "pEC50")
         (doc/select-columns {:A :gene :B :protein :D :temperature})
         (drop 1)
         (map
          (fn [d]
            (-> d
                (update :gene #(str/replace % "#" ""))
                (update :protein #(str/replace % "#" "")))))
         (group-by :protein)
         (filter (fn [[protein genes]]
                   (not (= 1 (count (distinct (map :gene genes)))))))
         (map (fn [[protein genes]]
                [protein (sort-by :temperature genes)]))))

  (def non-10-data-points-proteins
    (->> ecoli-raw
         (doc/select-sheet "pEC50")
         (doc/select-columns {:A :gene :B :protein :D :temperature})
         (drop 1)
         (map
          (fn [d]
            (-> d
                (update :gene #(str/replace % "#" ""))
                (update :protein #(str/replace % "#" "")))))
         (group-by :protein)
         (filter (fn [[protein genes]]
                   (not= 10 (count genes))))
         (map (fn [[protein genes]]
                [protein (sort-by :temperature genes)]))))

  ;; temperature distinct count => 10
  '(42.0 45.4 49.0 51.9 54.8 57.9 60.5 63.6 67.0 71.3)
  ;; gene distinct count => 2920
  ;; protein distinct count => 3019
  )
