(ns data-cleaning
  (:require
   [fast-edn.core :as edn]
   [clojure.math :as math]
   [csv-utils :as csv]
   [clojure.string :as str]))

(defn numerify
  [m]
  (update-vals m
               #(let [n (try (edn/read-string %)
                             (catch Exception _))]
                  (if (or (number? n)
                          (nil? n))
                    n
                    %))))

(defn filter-insanity-fn
  [rules]
  (fn [m]
    (not
     (first
      (filter true?
              (for [[k rule] rules]
                (rule (get m k))))))))


(comment

  (defn effect-size
    [slope RSS0 RSS1]
    (* (if (pos? slope) 1 -1)
       (math/sqrt (- RSS0 RSS1))))

  (defn log2 [n]
    (/ (math/log n) (math/log 2)))

  (defn log-transformed-F-statistic
    [F-statistic]
    (log2 (+ F-statistic 1)))

  (defn get-gene-name->protein-id-mapping
    [raw-table]
    (->> raw-table
         (map (juxt :gene_name :protein_id))
         distinct
         (into {})))

  (defn preprocess
    [raw processed]
    (let [gene-name->protein-id
          (get-gene-name->protein-id-mapping raw)]
      (->> processed
           (map (fn [row]
                  {:gene_name   (:clustername row)
                   :protein_id  (gene-name->protein-id (:clustername row))
                   :effect_type (:detected_effectH1 row)
                   :effect_size (apply effect-size
                                       ((juxt :slopeH1
                                              :rssH0
                                              :rssH1) row))
                   :log_transformed_f_statistic
                   (log-transformed-F-statistic (:F_statistic row))
                   :fdr         (:FDR row)})))))

  (let [raw-file         "resources/tpp-raw-amikacin-pae.csv"
        processed-file   "resources/tpp-processed-amikacin-pae.csv"
        output-file-name "tpp-amikacin-pae.csv"
        raw
        (->> (csv/read-csv-data raw-file)
             (transduce
              (comp
               (filter (filter-insanity-fn
                        {:protein_id #(str/starts-with? % "#")}))
               (map numerify))
              conj
              []))
        processed
        (->> (csv/read-csv-data processed-file)
             (transduce
              (comp
               (map numerify))
              conj
              []))]
    (csv/write-csv-data! output-file-name (preprocess raw processed))))
