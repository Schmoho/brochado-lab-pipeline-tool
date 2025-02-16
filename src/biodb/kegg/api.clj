(ns biodb.kegg.api
  (:require
   [cheshire.core :as json]
   [clj-http.client :as client]
   [clojure.java.io :as java.io]
   [clojure.data.csv :as csv]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.shell :as sh]
   [clojure.tools.logging :as log]
   [clojure.set :as set]
   [clojure.java.io :as io]
   [biodb.http :as http]))

(def kegg-api-base "https://rest.kegg.jp")

(def kegg-organisms-in-ncbi-brite-id "br:br08610")

(defn list
  [database]
  (-> (http/get (format "%s/list/%s" kegg-api-base database))
      :body))

#_(str/split-lines (list "br:08908"))

(defn get
  [id]
  (log/debug "Get entity" id "from KEGG.")
  (-> (http/get (format "%s/get/%s" kegg-api-base id))
      :body))

(defn get-orthology
  [id]
  (-> (http/get (format "%s/get/ko:%s" kegg-api-base id))
      :body))

(defn get-pathway
  [id]
  (-> (http/get (format "%s/get/pathway:%s" kegg-api-base id))
      :body))

(defn get-json
  [id]
  (-> (http/get (format "%s/get/%s/json" kegg-api-base id))
      :body
      (json/parse-string true)))


(defn find
  [database query]
  (-> (http/get (format "%s/find/%s/%s" kegg-api-base database query))
      :body))

;; (str/split-lines (find "brite" "taxonomy"))

;; (->> (tree-seq
;;       map?
;;       :children
;;       (get-json kegg-organisms-in-ncbi-brite-id))
;;      (filter (fn [e]
;;                (and (map? e) (:name e)
;;                     (str/includes? (:name e) "[TAX:"))))
;;      (map (fn [stuff]
;;             [(second (re-find #"\[TAX:(\d+)\]" (:name stuff)))
;;              #_(map (comp first #(str/split % #"\s+") :name) (:children stuff))
;;              (:children stuff)]))
;;      (filter (fn [[ncbi-tax-id stuff]]
;;                (> (count stuff) 1 ))))

(defn link
  [target-database source-database]
  (-> (http/get (format "%s/link/%s/%s" kegg-api-base target-database source-database))
      :body))

#_(->> (kegg-list "genome")
     (filter (fn [[_ _ org-name]]
               (when org-name 
                 (str/includes?
                  (str/lower-case org-name)
                  "pseudomonas aeruginosa")))))

#_(->> (kegg-list "genome"))

;; (defn get!
;;   [query]
;;   (log/info (format "KEGG API - get %s" query))
;;   (let [result (-> (client/get (format "%s/get/%s" base-url query))
;;                    :body
;;                    parse-kegg-get-result)]
;;     (if (= 1 (count result))
;;       (first result)
;;       result)))

;; (defn get-ids!
;;   [ids]
;;   (->> (for [ids (partition-all 10 ids)]
;;          (get! (str/join "+" ids)))
;;        (apply concat)))

(defn get-all-genes!
  [organism]
  #_(.start
     (Thread.
      (fn []
        (->> (kegg-list organism)
             (map first)
             (map (comp parse-kegg-get-result kegg-get))
             (json/generate-string)
             (spit (format "resources/kegg/genes/%s.json" organism)))))))

(defn get-all-pathways!
  [organism]
  #_(let [pathways (fn pathways
                     [kegg-data]
                     (->> kegg-data
                          (map :PATHWAY)
                          (map (partial map first))
                          (apply concat)
                          set))]
      (.start
       (Thread.
        (fn []
          (->> (pathways pseudo-data)
               (map (comp parse-kegg-get-result kegg-get))
               (json/generate-string)
               (spit (format "resources/kegg/pathways/%s.json" organism))))))))

