(ns schmoho.formats.pdb
  (:require
   [clojure.string :as str]
   #?(:clj [schmoho.utils.file :as utils])))

(defn pad-to-80 [s]
  (let [len (count s)]
    (if (< len 80)
      (str s (apply str (repeat (- 80 len) " ")))
      (subs s 0 80))))

(def pdb-map
  {"HEADER" [[1 6 :record-name] [11 50 :classification] [51 59 :deposition-date] [63 66 :id-code]]
   "OBSLTE" [[1 6 :record-name] [9 10 :continuation] [12 20 :replacement-date] [22 25 :replacement-id-code-1] [32 35 :replacement-id-code-2] [37 40 :replacement-id-code-3] [42 45 :replacement-id-code-4] [47 50 :replacement-id-code-5] [52 55 :replacement-id-code-6] [57 60 :replacement-id-code-7] [62 65 :replacement-id-code-8] [67 70 :replacement-id-code-9] [72 75 :replacement-id-code-10]]
   "TITLE"  [[1 6 :record-name] [9 10 :continuation] [11 79 :title]]
   "SPLIT"  [[1 6 :record-name] [9 10 :continuation] [12 15 :related-id-code-1] [17 20 :related-id-code-2] [22 25 :related-id-code-3] [27 30 :related-id-code-4] [32 35 :related-id-code-5] [37 40 :related-id-code-6] [42 45 :related-id-code-7] [47 50 :related-id-code-8] [52 55 :related-id-code-9] [57 60 :related-id-code-10]]
   "SPRSDE" [[1 6 :record-name] [9 10 :continuation] [12 20] [22 25] [32 35] [37 40] [42 45] [47 50] [52 55] [57 60] [62 65] [67 70] [72 75]]
   "AUTHOR" [[1 6 :record-name] [9 10 :continuation] [11 79 :author-list]]
   "KEYWDS" [[1 6 :record-name] [9 10 :continuation] [11 79 :keywords]]
   "JRNL"   [[1 6 :record-name] [13 76 :text]]
   "COMPND" [[1 6 :record-name] [8 10 :continuation] [11 79 :compound]]
   "SOURCE" [[1 6 :record-name] [8 10 :continuation] [11 79 :source-name]]
   "REMARK" [[1 6 :record-name] [8 10 :remark-number] [12 79 :text]]
   "MODEL"  [[1 6 :record-name] [11 14 :serial]]
   "ATOM"   [[1 6 :record-name]
             [7 11 :atom-serial-number parse-long]
             [13 16 :name]
             [17 17 :alternate-location]
             [18 20 :residue-name]
             [22 22 :chain-id]
             [23 26 :residue-sequence-number parse-long]
             [27 27 :insertion-code]
             [31 38 :x parse-double]
             [39 46 :y parse-double]
             [47 54 :z parse-double]
             [55 60 :occupancy]
             [61 66 :temperature-factor parse-double]
             [77 78 :element]
             [79 80 :charge]]})

(defn parse-pdb-line
  [index-map line]
  (let [record-type  (subs line 0 6)
        trimmed-type (str/trim record-type)
        fields       (get index-map trimmed-type)]
    (if fields
      (reduce (fn [acc [start end key parse-fn]]
                (let [value (-> (subs line (dec start) end)
                                str/trim
                                not-empty
                                ((fn [x]
                                   (if (and x parse-fn)
                                     (parse-fn x)
                                     x))))]
                  (assoc acc key value)))
              {}
              fields)
      line)))

(defmulti parsed-pdb type)

#?(:clj
   (defmethod parsed-pdb java.lang.String
     [pdb-string]
     (->> pdb-string
          str/split-lines
          (mapv (comp (partial parse-pdb-line pdb-map)
                      pad-to-80))))

   (defmethod parsed-pdb java.io.File
     [pdb-file]
     (->> (slurp pdb-file)
          str/split-lines
          (mapv (comp (partial parse-pdb-line pdb-map)
                      pad-to-80))))

   (defmethod parsed-pdb java.util.List
     [pdb-lines]
     (if (every? map? pdb-lines)
       pdb-lines
       (->> pdb-lines
            (mapv (comp (partial parse-pdb-line pdb-map)
                        pad-to-80))))))

(defn atoms
  [pdb]
  (->> (parsed-pdb pdb)
       (filter #(and (map? %)
                     (= (:record-name %) "ATOM")))))

(defn residue
  [pdb n]
  (-> (group-by :residue-sequence-number (atoms pdb))
      (get n)))

(defn center-of-residue
  [residue]
  (let [mean (fn [xs]
               (/ (apply + xs)
                  (count xs)))]
    {:x (mean (map :x residue))
     :y (mean (map :y residue))
     :z (mean (map :z residue))}))

(defmulti filter-tail-regions (fn [filter-fn pdb] (type pdb)))

#?(:clj
   (defmethod filter-tail-regions java.io.File
     [filter-fn pdb]
     (let [intermediate-file (utils/create-temp-file "pdb")]
       (->> (file-seq pdb)
            (filter-tail-regions filter-fn)
            (str/join "\n")
            (spit intermediate-file)))))
#?(:clj
   (defmethod filter-tail-regions java.lang.String
     [filter-fn pdb]
     (->> pdb
          str/split-lines
          (filter-tail-regions filter-fn)
          (str/join "\n"))))

#?(:clj
   (defmethod filter-tail-regions java.util.List
     [filter-fn pdb-lines]
     (let [parse                          (if (every? map? pdb-lines) identity (partial parse-pdb-line pdb-map))
           non-atoms-start                (->> pdb-lines
                                               (take-while (comp
                                                            #(not= "ATOM" (:record-name %))
                                                            parse)))
           non-atoms-end                  (->> pdb-lines
                                               reverse
                                               (take-while (comp
                                                            #(not= "ATOM" (:record-name %))
                                                            parse))
                                               reverse)
           atoms                          (->> pdb-lines
                                               (filter (comp
                                                        #(= "ATOM" (:record-name %))
                                                        parse)))
           filtered-atoms-from-beginning  (->> atoms
                                               (drop-while (comp
                                                            filter-fn
                                                            parse)))
           filtered-atoms-from-both-sides (->> filtered-atoms-from-beginning
                                               reverse
                                               (drop-while (comp
                                                            filter-fn
                                                            parse))
                                               reverse)]
       (concat non-atoms-start
               filtered-atoms-from-both-sides
               non-atoms-end))))

#_(->> (io/file "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9.pdb")
     (io/reader)
     (line-seq)
     (filter-tail-regions #(> 70 (:temperature-factor %)))
     (str/split-lines))

#_(parsed-pdb (slurp "results/51e4e6fa-eac0-4a2c-9490-3252a785dd6a/A0A0H2ZHP9.pdb"))
