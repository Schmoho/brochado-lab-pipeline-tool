(ns openapi
  (:require
   [cheshire.core :as json]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clj-http.client :as client]
   [clojure.string :as str]))


;; (def ncbi (yaml/parse-string (slurp (io/file "resources/openapi/ncbi.yaml"))))

;; (def path-element (-> ncbi :paths first))

(defn path
  [path-element]
  (-> path-element first name))

(defn path-parameters
  [path-element]
  (let [path (name (first path-element))
        pattern #"\{(.*?)\}"]
    (map second (re-seq pattern path))))

(defn get-request
  [path-element]
  (-> path-element second :get))

(defn query-parameters
  [get-request]
  (->> get-request
       :parameters
       (group-by (comp #(if % :required :optional)
                       :required))))

;; (-> path-element get-request query-parameters)

;; (let [
;;       operation-id                (:operationId get-request)]
;;   `(defn ~(symbol operation-id)
;;      [~@(map (comp symbol :name) required) ~'opts]
;;      (client/get ~(str/replace "genome/accession/{accessions}/download_summary"
;;              "{accessions}"
;;              (str/join "," ))(str (-> ncbi :servers first :url)
;;                        "/"
;;                        operation-id))))

;; ncbi

;; (client/get (-> ncbi :servers first :url))

;; (clj-http.client/get
;;  "https://api.ncbi.nlm.nih.gov/datasets/v2/genome_download_summary" {:query-params {"accessions" ["GCF_000001405.40"]}})
