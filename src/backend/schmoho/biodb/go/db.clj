(ns schmoho.biodb.go.db
  (:require
   [clojure.java.io :as java.io]
   [clojure.string :as str]))

(defn parse-gaf
  [gaf-filename]
  (->> (java.io/reader gaf-filename)
       (line-seq)
       (drop-while #(str/starts-with? (str %) "!"))
       (map #(zipmap
              [:db :db-object-id :db-object-symbol :relation :go-id :db-reference
              :evidence-code :with-or-from :aspect :db-object-name :db-object-synonym
              :db-object-type :taxon :date :assigned-by :annotation-extension :gene-product-form-id]
              (str/split % #"\t")))))

#_(def pseudocap-gaf (parse-gaf "resources/go/pseudocap.gaf"))

(defn proteins->terms
  [gaf]
  (->> gaf
       (filter #(= "protein" (:db-object-type %)))
       (group-by :db-object-id)
       (into {})))

;; (def db
;;   (let [eco-annotations (parse-gaf (java.io/resource "go/QuickGO-annotations-ecoli-83333.gaf"))
;;         pau-annotations (parse-gaf (java.io/resource "go/QuickGO-annotations-pseudomonas-208963.gaf"))]
;;     (atom {:eco {:annotations eco-annotations
;;                  :proteins    (proteins->terms eco-annotations)}
;;            :pau {:annotations pau-annotations
;;                  :proteins    (proteins->terms pau-annotations)}
;;            :go  (json/parse-string (slurp (java.io/resource "go/go.json")))})))
