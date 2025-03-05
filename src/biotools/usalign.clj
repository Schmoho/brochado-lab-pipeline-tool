(ns biotools.usalign
  (:require
   [biotools.utils :refer :all]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

(is-command-available? "usalign")

(defn files-with-ending
  "Path is a string, ending needs to contain the dot."
  [path ending]
  (->> (file-seq (io/file path))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ending))))


(defn read-between-markers
  [o start-marker end-marker]
  (loop [lines   (str/split-lines o)
         inside? false
         result  []]
        (if (empty? lines)
          result
          (let [line            (first lines)
                remaining-lines (rest lines)]
            (cond
              (and (not inside?) (= line start-marker))   (recur (rest remaining-lines) true result)
              (and inside? (.startsWith line end-marker)) result
              inside?                                     (recur remaining-lines inside? (conj result line))
              :else                                       (recur remaining-lines inside? result))))))


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
