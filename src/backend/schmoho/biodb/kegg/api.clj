(ns schmoho.biodb.kegg.api
  (:require
   [schmoho.biodb.http :as http]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schmoho.biodb.kegg.parser :as kegg.parser]
   [schmoho.utils.file :as utils]))

(def kegg-api-base "https://rest.kegg.jp")

(def kegg-organisms-in-ncbi-brite-id "br:br08610")

;; ### Simple Wrappers ###

(defn get-list
  [database]
  (-> (http/get (format "%s/list/%s" kegg-api-base database))
      :body))

(defn get-entity
  [id]
  (log/debug "Get entity" id "from KEGG.")
  (-> (http/get (format "%s/get/%s" kegg-api-base id))
      :body))

(defn get-ids
  [ids]
  (->> (for [ids (partition-all 10 ids)]
         (get-entity (str/join "+" ids)))
       (apply concat)))

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

(defn find-query
  [database query]
  (-> (http/get (format "%s/find/%s/%s" kegg-api-base database query))
      :body))

(defn link
  [target-database source-database]
  (-> (http/get (format "%s/link/%s/%s" kegg-api-base target-database source-database))
      :body))

;; ### Convenience Wrappers ###

(defn get-ncbi-tax-id->kegg-organism-mapping
  []
  (->> (tree-seq
       map?
       :children
       (get-json kegg-organisms-in-ncbi-brite-id))
      (filter (fn [e]
                (and (map? e)
                     (:name e)
                     (str/includes? (:name e) "[TAX:"))))
      (map (fn [stuff]
             [(second (re-find #"\[TAX:(\d+)\]" (:name stuff)))
              (map
               (fn [m]
                 (zipmap
                  [:kegg-id :name]
                  (str/split (:name m) #"\s+" 2)))
               (:children stuff))]))
      (into {})))

(defn get-all-genes!
  [organism]
  (future
    (->> (get-list organism)
         #_(map first)
         #_(map (comp parse-kegg-get-result kegg-get))
         #_(json/generate-string)
         #_(spit (format "resources/kegg/genes/%s.json" organism)))))

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

(def download-kegg-xform
   (comp
    (partition-all 10)
    (map (comp kegg.parser/parse-kegg-get-result get-entity (partial str/join "+")))))

(comment
  (def ncbi-tax-id->kegg-organism
    (get-ncbi-tax-id->kegg-organism-mapping))

  (defn ncbi-taxon->kegg-organism
    [search-taxon]
    (let [uniprot-taxa     (concat
                            (map (comp str :taxonId)
                                 (:lineage search-taxon))
                            [(-> search-taxon :taxonId str)])]
      (->> uniprot-taxa
           (map (comp (juxt identity ncbi-tax-id->kegg-organism)))
           reverse
           (filter (comp some? second))
           first)))

  (->> user/brochado-strains
       (filter :ncbi-taxonomy-id)
       (map (juxt :ncbi-taxonomy-id :kegg-id)))
  ;; => (["679895" "eco"]
  ;;     ["585034" "ecr"]
  ;;     ["208964" "pae"]
  ;;     ["208963" "pau"]
  ;;     ["99287" "stm"]
  ;;     ["588858" "seo"])

  (map (comp count str/split-lines get-list) ["eco" "ecr" "pae" "pau" "stm" "seo"])

  (->> (get-list "eco")
       (kegg.parser/parse-genome-list)
       (take 12)
       (transduce
        (comp (map first)
              download-kegg-xform)
        conj
        [])
       (apply concat)
       #_(utils/write! "kegg-eco.edn"))

  (str/split-lines (get-list "br:08908"))

  (str/split-lines (find-query "brite" "taxonomy"))

  (ncbi-tax-id->kegg-organism "208964")

  (->> (get-list "genome")
       (str/split-lines)
       (filter (fn [s]
                 (str/includes?
                  (str/lower-case s)
                  "pseudomonas aeruginosa"))
               #_(fn [[_ _ org-name]]
                   (when org-name
                     (str/includes?
                      (str/lower-case org-name)
                      "pseudomonas aeruginosa"))))))
