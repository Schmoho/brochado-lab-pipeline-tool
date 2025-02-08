(ns kegg.api
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
   [clojure.java.io :as io]))

(def kegg-api-base "https://rest.kegg.jp")

(defn parse-genome-list
  [list]
  (->> list
       (str/split-lines)
       (map #(str/split % #"\t"))
       (map (fn [[t-number organism-name]]
              (->> (str/split organism-name #";" 2)
                   (map str/trim)
                   (concat [t-number]))))))

(defn parse-organism-list
  [list]
  (str/split-lines)
     (map (comp
           (fn [[t-number kegg-code scientific-name lineage]]
             [t-number kegg-code scientific-name (vec (str/split lineage #";"))])
           #(str/split % #"\t"))))

(def kegg-list
  (memoize
   (fn kegg-list
     [database]
     (let [result (-> (client/get (format "%s/list/%s" kegg-api-base database))
                      :body)]
       (case database
         "genome" (parse-genome-list result)
         "organism" (parse-organism-list result)
         result)))))

#_(->> (kegg-list "genome")
     (filter (fn [[_ _ org-name]]
               (when org-name 
                 (str/includes?
                  (str/lower-case org-name)
                  "pseudomonas aeruginosa")))))

(->> (kegg-list "genome"))




(def pseudomonas "pau")
(def ecoli "eco")

(defn- update-if-exists
  [m k f]
  (if (contains? m k)
    (update m k f)
    m))

(def two-part (partial map #(str/split % #"\s+" 2)))

(defn split-bulk-response
  [lines]
  (let [idx (inc (.indexOf lines "///"))
        [head tail] (split-at idx lines)]
    (if (not-empty tail)
      (concat [(drop-last head)] (lol tail))
      [(drop-last head)])))

(defn- parse-kegg-get-result
  [body]
  (let [responses (->> body
                       (str/split-lines)
                       split-bulk-response)
        
        prepped-responses
        (->> responses
             (map (fn [response]
                    (->> response
                         (map #(if (> (count %) 12)
                                 [(subs % 0 12)
                                  (subs % 12)]
                                 [%]))
                         (reduce (fn [acc [tag value]]
                                   (if (str/blank? (str/trim tag))
                                     (update acc
                                             (dec (count acc))
                                             #(conj % value))
                                     (conj acc [tag value])))
                                 [])
                         (map (fn [[k & vs]]
                                [(str/trim k) vs]))
                         (into {})))))

        parsed-responses
        (->> prepped-responses
             (map (fn [response]
                    (-> response
                        (update-if-exists "ORTHOLOGY" two-part)
                        (update-if-exists "DBLINKS" (partial map #(str/split % #":\s+" 2)))
                        #_(update-if-exists "DBLINKS"
                                            (comp (partial into {})
                                                  (partial map
                                                           (fn [pair]
                                                             (when pair
                                                               (let [[k v]
                                                                     (map str/trim
                                                                          (str/split pair #":"))]
                                                                 [k v]))))))
                        (update-if-exists "NTSEQ" (comp (partial apply str)
                                                        (partial drop 1)))
                        (update-if-exists "AASEQ" (comp (partial apply str)
                                                        (partial drop 1)))
                        (update-if-exists "ORGANISM" two-part)
                        (update-if-exists "PATHWAY" two-part)
                        (update-if-exists "GENE" two-part)
                        (update-if-exists "REL_PATHWAY" two-part)
                        (update-if-exists "COMPOUND" two-part)
                        (update-if-exists "PATHWAY_MAP" two-part)
                        (update-if-exists "MODULE" two-part)))))]
    parsed-responses))

(defn get!
  [query]
  (log/info (format "KEGG API - get %s" query))
  (let [result (-> (client/get (format "%s/get/%s" base-url query))
                   :body
                   parse-kegg-get-result)]
    (if (= 1 (count result))
      (first result)
      result)))

(defn get-ids!
  [ids]
  (->> (for [ids (partition-all 10 ids)]
         (get! (str/join "+" ids)))
       (apply concat)))

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

