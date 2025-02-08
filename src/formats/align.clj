(ns align
  (:require
   [clojure.set :as set]
   [utils :refer :all]
   [pdb :as pdb]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [cheshire.core :as json]))

(is-command-available? "usalign")

(defn parallel-common-residues
  [pdb1 pdb2]
  (let [bb1         (pdb/backbone pdb1)
        bb2         (pdb/backbone pdb1)
        seq-numbs1  (set (->> bb1
                              (map :residue-sequence-number)
                              distinct))
        seq-numbs2  (set (->> bb2
                              (map :residue-sequence-number)
                              distinct))
        common-nums (set/intersection seq-numbs1 seq-numbs2)]
    [(filter #(contains? common-nums (:residue-sequence-number %)) bb1)
     (filter #(contains? common-nums (:residue-sequence-number %)) bb2)]))

(defn backbone-distances
  [pdb1 pdb2]
  (->> (parallel-common-residues pdb1 pdb2)
       (apply map (fn [a b]
                    [(:residue-sequence-number a)
                     (pdb/distance a b)]))))


(defn align-3d-structure!
  "Outputs the aligned structure as a pdb-file in the given folder with given name.
  Returns parsed aligned PDB structure and a table of the aligned backbone atom distances."
  [from-pdb to-pdb output-folder output-file]
  (.mkdirs (io/file output-folder))
  (binding [sh/*sh-dir* output-folder]
    (let [from-pdb-abs       (.getAbsolutePath (io/file from-pdb))
          to-pdb-abs         (.getAbsolutePath (io/file to-pdb))
          result-1           (sh/sh "usalign" from-pdb-abs to-pdb-abs "-o" (str/replace (.getAbsolutePath output-file) ".pdb" ""))
          result-2           (sh/sh "usalign" from-pdb-abs to-pdb-abs "-do")
          backbone-distances (->>  (read-between-markers
                                    (:out result-2)
                                    "#Aligned atom 1\tAligned atom 2 \tDistance#"
                                    "###############\t###############\t#########")
                                   (map (comp
                                         (fn [x]
                                           [(parse-long (nth x 3))
                                            (parse-long (nth x 7))
                                            (parse-double (nth x 8))])
                                         #(str/split % #"\s+")
                                         str/trim))
                                   (filter #(= (first %) (second %)))
                                   (map (fn [[residue-number _ distance]]
                                          [residue-number distance])))
          aligned-pdb        (->> output-file
                                  slurp
                                  pdb/parsed-pdb)]
      (do (doseq [f (->> (files-with-ending output-folder "pml")
                         (filter #(str/includes? % (str/replace (.getName (io/file output-file))
                                                                ".pdb"
                                                                ""))))]
            (.delete f))
          #_(spit (str output-folder "/" output-file "-backbone-distances.json") (json/generate-string backbone-distances))
          {:result             result-1
           :backbone-distances backbone-distances
           :aligned-pdb        aligned-pdb}))))
