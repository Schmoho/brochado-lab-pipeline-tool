(ns formats.fasta
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::sequence string?)

(s/def ::header string?)

(s/def ::fasta
  (s/keys :req-un [::header ::sequence]))

(defmulti ->fasta meta)

(defn ->fasta-string
  ([fasta-records] (->fasta-string :fasta/sequence fasta-records))
  ([sequence-accessor fasta-records]
   (str/join "\n"
             (->> fasta-records
                  (map (fn [record] (str (:fasta/header record)
                                         "\n"
                                         (sequence-accessor record))))))))

