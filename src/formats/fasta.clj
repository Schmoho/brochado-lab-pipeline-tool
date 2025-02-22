(ns formats.fasta
  (:require [clojure.spec.alpha :as s]))

(s/def ::sequence string?)

(s/def ::header string?)

(s/def ::fasta
  (s/keys :req-un [::header ::sequence]))

(defmulti ->fasta meta)
