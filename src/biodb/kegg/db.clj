(ns kegg.db
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.java.io :as io]))

(defn parse-genes-file
  [genes-file]
  (let [parsed (-> (io/file genes-file)
                   (slurp)
                   (json/parse-string true))]
    (zipmap
     (map (comp first #(str/split % #"\s+") first :ENTRY)
          parsed)
     parsed)))

(def parse-pathways-file parse-genes-file)

(def uniprot-id (comp :UniProt :DBLINKS))
(defn uniprot-id->gene-lookup
  [genes]
  (->> genes
       (map (fn [[gene-id gene]]
              (when-let [uniprot-id (uniprot-id gene)]
                [uniprot-id gene])))
       (filter some?)
       (into {})))

(defn gene-id->symbol-lookup
  [genes]
  (->> genes
       (map (fn [[_ gene]]
              (when-let [sym (-> gene :SYMBOL first)]
                {sym [gene]})))
       (filter some?)
       (reduce (partial merge-with concat))))

(defn prepare-set!
  [organism]
  (let [genes            (parse-genes-file (io/resource (format "kegg/genes/%s.json" organism)))
        genes-by-symbols (gene-id->symbol-lookup genes)
        pathways         (parse-pathways-file (io/resource (format "kegg/pathways/%s.json" organism)))]
    ;; SANITY CHECK dass die ID-sets disjunkt sind!
    {:genes    (merge genes-by-symbols genes)
     :pathways pathways
     :proteins (uniprot-id->gene-lookup genes)}))

(def db
  (atom
   {:eco       (prepare-set! "eco")
    :pau       (prepare-set! "pau")
    :orthology (prepare-set! "orthology")}))

(defn genes
  ([organism]
   (-> @db (get (keyword organism)) :genes))
  ([organism id-list]
   (-> @db (get (keyword organism)) :genes (select-keys id-list))))

(defn pathways
  ([organism]
   (-> @db (get (keyword organism)) :pathways))
  ([organism id-list]
   (-> @db (get (keyword organism)) :pathways (select-keys id-list))))

(defn proteins
  ([organism]
   (-> @db (get (keyword organism)) :proteins))
  ([organism id-list]
   (-> @db (get (keyword organism)) :proteins (select-keys id-list))))

(defn by-id
  [id]
  (let [results (->> (vals @db)
                     (map vals)
                     flatten
                     (map
                      (fn [data-set]
                        (get data-set id)))
                     (filter some?))]
    (if-not (= 1 (count results))
      (throw (ex-info "Weird stuff" {:results results}))
      (first results))))

(defn ortholog
  [entry]
  (if (and (not (map-entry? entry))
           (not (map? entry))
           (seqable? entry))
    (map ortholog entry)
    (let [entry (cond
                  (string? entry)    (by-id entry)
                  (map-entry? entry) (second entry)
                  :else              entry)]
      (cond
        (:KO_PATHWAY entry) (-> @db :orthology :pathways (get (first (:KO_PATHWAY entry))))
        (:ORTHOLOGY entry)  (-> @db :orthology :genes (get (ffirst (:ORTHOLOGY entry))))))))

;; Der Hintergrund von dem Bums hier war, dass
;; die Liste der Gene die ein Pathway führt
;; nicht kongruent ist zu der Liste der Pathways
;; die ein Gen führt
#_(defn affected-pathways
  [pathway-lookup gene-name-lookup hits pathways]
  ;; besser wäre: [organism gene-list]
  (->> (map :gene hits)
       (map
        (fn [gene]
          (let [pathways           (pathway-lookup gene)
                looked-up          (gene-name-lookup gene)
                looked-up-pathways (map pathway-lookup looked-up)
                final-ps           (->> (cond pathways           pathways
                                              looked-up-pathways looked-up-pathways
                                              :else nil)
                                        (filter some?)
                                        flatten
                                        distinct)]
            (->> (repeat (vector gene))
                 (interleave final-ps)
                 (partition 2)
                 (map vec)
                 (into {})))))
       (filter not-empty)
       (reduce (partial merge-with concat))
       (map (fn [[pathway genes]]
              [(-> pathway pathways :KO_PATHWAY first)
               {:name (-> pathway
                          pathways
                          :NAME
                          first
                          (str/replace " - Pseudomonas aeruginosa UCBPP-PA14" "")
                          (str/replace " - Escherichia coli K-12 MG1655" ""))
                :genes genes}]))
       (into {})))

#_(defn gene->pathway-lookup
  [pathway-lookup]
  (->> pathway-lookup
       (map (fn [[pathway-id data]]
              (zipmap
               (map first (:GENE data))
               (repeat (vector pathway-id)))))
       (reduce (partial merge-with concat))))
