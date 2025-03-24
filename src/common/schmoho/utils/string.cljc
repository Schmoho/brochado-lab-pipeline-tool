(ns schmoho.utils.string
  (:require
   [clojure.string :as str]))

(defn get-hash [x]
  (str/replace (str (clojure.core/hash x)) "-" "0"))

(defn rand-str
  []
  (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz"))))
