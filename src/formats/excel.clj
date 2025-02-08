(ns excel
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [dk.ative.docjure.spreadsheet :as doc]
   [clojure.math :as math]
   [clojure.set :as set]))

#_(defonce ecoli-amikacin (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Ecoli.xlsx"))
#_(defonce ecoli-cefotaxime (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Ecoli.xlsx"))
#_(defonce pa-amikacin (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Paeruginosa.xlsx"))
#_(defonce pa-cefotaxime (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Paeruginosa.xlsx"))

#_(defonce ec-cefotaxime-raw (doc/load-workbook "tpp/2020-11-12_results_2D_TPP-cefotaxime-Ecoli.xlsx"))

(defn median_odd
  [vector]
  (nth vector (/ (count vector) 2)))

(defn median_even [vector]
  (let [middle-idx (/ (count vector) 2)]
    (/ (+ (nth vector middle-idx)
          (nth vector (dec middle-idx)))
       2)))

(defn median [vector]
  (if (even? (count vector))
    (median_even vector)
    (median_odd vector)))

(defn log2 [n]
  (/ (math/log n) (math/log 2)))

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

#_(->> ec-cefotaxime-raw
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

;; ohne
[0.20233699160632201 1.6968640655494749]
;; log-transformiert
[-2.3062161289284364 0.7621266242950857]

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


(def ec-amikacin-hits
  (->> (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Ecoli.xlsx")
       (volcano)
       (hits)
       (vec)))

(def ec-cefotaxime-hits
  (->> (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Ecoli.xlsx")
       (volcano)
       (hits)
       (vec)))

(def pa-amikacin-hits
  (->> (doc/load-workbook "tpp/Amikacin-fdr_analysis_nils-Paeruginosa.xlsx")
       (volcano)
       (hits)
       (vec)))

(def pa-cefotaxime-hits
  (->> (doc/load-workbook "tpp/Cefotaxime-fdr_analysis_nils-Paeruginosa.xlsx")
       (volcano)
       (hits)
       (vec)))


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

#_(write-volcano-plot-data! pa-amikacin "pa-amikacin.json")
#_(write-volcano-plot-data! pa-cefotaxime "pa-cefotaxime.json")
#_(write-volcano-plot-data! ecoli-amikacin "ec-amikacin.json")
#_(write-volcano-plot-data! ecoli-cefotaxime "ec-cefotaxime.json")
#_(write-hits! pa-amikacin "pa-amikacin-hits.json")
#_(write-hits! pa-cefotaxime "pa-cefotaxime-hits.json")
#_(write-hits! ecoli-amikacin "ec-amikacin-hits.json")
#_(write-hits! ecoli-cefotaxime "ec-cefotaxime-hits.json")

#_(set/difference
   (->> (volcano ecoli-cefotaxime)
        (sort-by :FDR)
        (filter #(< (:FDR %) 0.01))
        (map :gene)
        set)
   (set hits))


#_(spit "volcano.json"
        (json/generate-string ))

;; (->> ecoli-cefotaxime
;;      (doc/select-sheet "fdr_tab")
;;      (doc/select-columns {:A :gene})
;;      ;; stellt sich raus, das hier ist leer:
;;      #_(filter #(not= (:representative %) (:cluster %)))
;;      (drop 1)
;;      (map :gene)
;;      (take 3))


;; (def ecoli-raw
;;   (doc/load-workbook "ttp/2020-11-12_results_2D_TPP-cefotaxime-Ecoli.xlsx"))

;; (def non-1-to-1-mapped-genes-and-proteins
;;   (->> ecoli-raw
;;       (doc/select-sheet "pEC50")
;;       (doc/select-columns {:A :gene :B :protein :D :temperature})
;;       (drop 1)
;;       (map
;;        (fn [d]
;;          (-> d
;;               (update :gene #(str/replace % "#" ""))
;;               (update :protein #(str/replace % "#" "")))))
;;       (group-by :protein)
;;       (filter (fn [[protein genes]]
;;                 (not (= 1 (count (distinct (map :gene genes)))))))
;;       (map (fn [[protein genes]]
;;              [protein (sort-by :temperature genes)]))))

;; (def non-10-data-points-proteins
;;   (->> ecoli-raw
;;       (doc/select-sheet "pEC50")
;;       (doc/select-columns {:A :gene :B :protein :D :temperature})
;;       (drop 1)
;;       (map
;;        (fn [d]
;;          (-> d
;;               (update :gene #(str/replace % "#" ""))
;;               (update :protein #(str/replace % "#" "")))))
;;       (group-by :protein)
;;       (filter (fn [[protein genes]]
;;                 (not= 10 (count genes))))
;;       (map (fn [[protein genes]]
;;              [protein (sort-by :temperature genes)]))))

;; ;; temperature distinct count => 10
;; ;; (42.0 45.4 49.0 51.9 54.8 57.9 60.5 63.6 67.0 71.3)
;; ;; gene distinct count => 2920
;; ;; protein distinct count => 3019
