(ns fasta
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defn parse [[header sequence-lines]]
  {:header   (apply str header)
   :sequence (apply str sequence-lines)})

(defn filter-fasta-lines
  [f file-path]
  (with-open [rdr (io/reader file-path)]
    (->> (line-seq rdr)
         (partition-by #(str/starts-with? % ">"))
         (partition 2)
         (map parse)
         (map (partial f file-path))
         (filter some?)
         doall)))

(->> (file-seq (io/file (io/resource "Pseudomonas library/faa/Pseudomonas aeruginosa")))
     (filter #(.isFile %))
     (map (partial filter-fasta-lines
                   (fn pbp1b-filter
                     [file-path fasta-entry]
                     (when (re-find #"[Pp]enicillin[ -]binding protein 1[Bb]"
                                    (:header fasta-entry))
                       (update fasta-entry :header #(str ">" (.getName file-path) " " (subs % 1)))))))
     (apply concat)
     (reduce (fn [out {:keys [header sequence]}])))
