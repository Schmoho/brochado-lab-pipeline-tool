(ns schmoho.dasudopit.math
  (:require
   [clojure.math :as math]))

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

(defn mean [vector]
  (/ (reduce + vector)
     (count vector)))

(defn log2 [n]
  (/ (math/log n) (math/log 2)))
