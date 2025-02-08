(ns pdb
  (:require
   [clojure.string :as str]
   [clojure.math :as math]))

(defn pad-to-80 [s]
  (let [len (count s)]
    (if (< len 80)
      (str s (apply str (repeat (- 80 len) " ")))
      (subs s 0 80))))

(defn partition-line
  [line-map line]
  (let [first-word (first (str/split line #"\s+"))
        ranges     (get line-map first-word)]
    (if ranges
      (map #(subs line (dec (first %)) (second %)) ranges)
      line)))

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
   "ATOM"   [[1 6 :record-name] [7 11 :atom-serial-number parse-long] [13 16 :name] [17 17 :alternate-location] [18 20 :residue-name] [22 22 :chain-id] [23 26 :residue-sequence-number parse-long] [27 27 :insertion-code] [31 38 :x parse-double] [39 46 :y parse-double] [47 54 :z parse-double] [55 60 :occupancy] [61 66 :temperature-factor] [77 78 :element] [79 80 :charge]]})

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

(defn parsed-pdb
  [pdb-string]
  (->> pdb-string
       str/split-lines
       (map (comp (partial parse-pdb-line pdb-map)
                  pad-to-80))))

(defn atoms
  [pdb]
  (filter #(and (map? %)
                (= (:record-name %) "ATOM"))
          pdb))

(defn residue
  [pdb n]
  (get (group-by :residue-sequence-number (atoms pdb)) n))

(defn c-alpha
  [residue]
  (first (filter #(= (:name %) "CA") residue)))

(defn backbone [pdb]
  (->> (atoms pdb)
      (filter #(= (:name %) "CA"))))

(defn distance
  [{x1 :x y1 :y z1 :z}
   {x2 :x y2 :y z2 :z}]
  (math/sqrt (+ (math/pow (- x1 x2) 2)
                (math/pow (- y1 y2) 2)
                (math/pow (- z1 z2) 2))))

(defn residues-in-distance
  [center-residue-number d pdb]
  (let [center-residue (c-alpha (residue pdb center-residue-number))
        d-filter #(let [dist (distance center-residue
                                       %)]
                    (<= dist d))]
    (->> pdb atoms
         (filter d-filter))))

(defn residue-numbers-in-distance
  [center-residue-number d pdb]
  (->> (residues-in-distance center-residue-number d pdb)
       (map :residue-sequence-number)
                       distinct))


(defn pdb-atom-table
  [pdb-string]
  (->> (str/split-lines pdb-string)
       (filter #(str/starts-with? % "ATOM"))
       (map #(str/split % #"\s+"))))

(defn center-of-residue
  [residue]
  (let [xs (map (comp parse-double #(nth % 6)) residue)
        ys (map (comp parse-double #(nth % 7)) residue)
        zs (map (comp parse-double #(nth % 8)) residue)
        mean (fn [xs]
               (/ (apply + xs)
                  (count xs)))]
    [(mean xs)
     (mean ys)
     (mean zs)]))
