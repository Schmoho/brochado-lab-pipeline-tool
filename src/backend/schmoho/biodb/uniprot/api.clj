(ns schmoho.biodb.uniprot.api
  (:require
   [schmoho.utils.walk :as utils]
   [schmoho.utils.file :as file-utils]
   [schmoho.biodb.http :as http]
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clj-http.client :as clj-http.client]
   [clojure.java.io :as io]
   [fast-edn.core :as edn]))

(def uniprot-api-base "https://rest.uniprot.org")

;; --- Simple Endpoint Wrappers ---

;; ;; Taxonomy

(def taxon-meta
  {:biodb/source :uniprot
   :uniprot/type :taxon})

(def taxon-fields-of-interest
  ["common_name" "id" "lineage" "rank" "scientific_name" "statistics"])

(def taxonomy-entry
  (http/id-query
   (str (format "%s/taxonomy/" uniprot-api-base) "%s")
   taxon-meta))

#_(:lineage (taxonomy-entry "208964" {:fields taxon-fields-of-interest}))

;; ;; Proteomes

(def proteome-meta
  {:biodb/source :uniprot
   :uniprot/type :proteome})

(def proteome-fields-of-interest
  ["organism_id" "protein_count" "upid" "redundantTo"])

(def proteomes-entry
  (http/id-query
   (str (format "%s/proteomes/" uniprot-api-base) "%s")
   proteome-meta))

#_(proteomes-entry "UP000029103") ;; redundant proteome
#_(proteomes-entry "UP000014183")

(defn proteomes-search
  [query-params]
  (let [url       (format "%s/proteomes/search" uniprot-api-base)
        query-params (update query-params :fields #(if (coll? %)
                                                     (str/join "," %)
                                                     %))
        _         (log/debug "Query" url "with" query-params)
        proteomes (-> (http/get url {:query-params query-params})
                      (:body)
                      (json/parse-string)
                      (utils/white-space-safe-keywordize-keys)
                      :results)]
    (mapv #(with-meta % proteome-meta) proteomes)))

;; ;; Proteins

(def uniprotkb-entry-meta
  {:biodb/source :uniprot
   :uniprot/type :uniprotkb-entry})

(def protein-fields-of-interest
  ["accession"
   "cc_domain"
   "cc_pathway"
   "ec"
   "ft_act_site"
   "ft_binding"
   "ft_dna_bind"
   "ft_domain"
   "ft_region"
   "ft_site"
   "ft_topo_dom"
   "ft_transmem"
   "ft_zn_fing"
   "gene_names"
   "go"
   "protein_existence"
   "protein_families"
   "protein_name"
   "sequence"
   "structure_3d"
   "xref_alphafolddb"
   "xref_biocyc"
   "xref_drugbank"
   "xref_drugcentral"
   "xref_interpro"
   "xref_kegg"
   "xref_pdb"
   "xref_unipathway"])

(def uniprotkb-entry
  (http/id-query
   (str (format "%s/uniprotkb/" uniprot-api-base) "%s")
   uniprotkb-entry-meta))

#_(uniprotkb-entry "G3XCV0" #_{:fields protein-fields-of-interest})

(defn uniprotkb-search
  [query-params]
  (let [url               (format "%s/uniprotkb/search" uniprot-api-base)
        _                 (log/debug "Query" url "with" query-params)
        uniprotkb-entries (-> (http/get url {:query-params query-params})
                              (:body)
                              (json/parse-string)
                              (utils/white-space-safe-keywordize-keys)
                              :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) uniprotkb-entries)))

#_(uniprotkb-search {:query "proteome:UP000002438"})

#_(uniprotkb-search {:query (format "gene:%s AND taxonomy_name:%s"
                                    "mrcb" "Pseudomonas aeruginosa")})

(defn uniprotkb-stream
  [query-params]
  (let [url               (format "%s/uniprotkb/stream" uniprot-api-base)
        _                 (log/debug "Query" url "with" query-params)
        uniprotkb-entries (-> (clj-http.client/get url {:query-params query-params})
                              (:body)
                              (json/parse-string)
                              (utils/white-space-safe-keywordize-keys)
                              :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) uniprotkb-entries)))

#_(uniprotkb-stream {:query "proteome:UP000002438"})

;; Uniparc

(def uniparc-entry-meta
  {:biodb/source             :uniprot
   :uniprot/type             :uniparc-entry})

(def uniparc-entry
  (http/id-query
   (str (format "%s/uniparc/" uniprot-api-base) "%s")
   uniparc-entry-meta))

#_(uniparc-entry "UPI00053A1130")

;; ;; Uniref

(def uniref-meta
  {:biodb/source :uniprot
   :uniprot/type :uniref-entry})

(def uniref-entry
  (http/id-query
   (str (format "%s/uniref/" uniprot-api-base) "%s")
   uniref-meta))

#_(uniref-entry #_"UniRef50_Q4K603"
                "UniRef90_G3XD31"
                #_"UniRef100_A0A0U4NUB5")

(defn uniref-search
  [query-params]
  (let [url     (format "%s/uniref/search" uniprot-api-base)
        _       (log/debug "Query" url "with" query-params)
        entries (-> (http/get url {:query-params query-params})
                    (:body)
                    (json/parse-string)
                    (utils/white-space-safe-keywordize-keys)
                    :results)]
    (mapv #(with-meta % uniref-meta) entries)))

#_(uniref-search {:query "uniprot_id:A0A0H2ZHP9"})

;; ;; Databases

(def database-meta
  {:biodb/source :uniprot
   :uniprot/type :database-reference})

(def databases
  (memoize
   (fn databases
     []
     (let [url         "https://rest.uniprot.org/database/stream?format=json&query=%28*%29"
           _           (log/debug "Query" url)
           http-result (clj-http.client/get url)
           databases   (-> http-result
                           :body
                           (json/parse-string)
                           (utils/white-space-safe-keywordize-keys)
                           :results)]
       (mapv #(with-meta % database-meta) databases)))))


;; --- Convenience Wrappers ---

(defn downstream-lineage
  [taxon-id]
  (let [url         (format "%s/taxonomy/stream" uniprot-api-base)
        _           (log/debug "Query" url "for" taxon-id)
        http-result (clj-http.client/get
                     url
                     {:query-params
                      {:query (format "ancestor:%s" taxon-id)}})
        taxons      (-> http-result
                        :body
                        (json/parse-string)
                        (utils/white-space-safe-keywordize-keys)
                        :results)]
    taxons))

#_(count (downstream-lineage 136841))

(defn proteins-by-proteome
  [proteome-id]
  (let [url         (format "%s/uniprotkb/stream" uniprot-api-base)
        _           (log/debug "Query" url "for" proteome-id)
        http-result (clj-http.client/get
                     url
                     {:query-params
                      {:query (format "proteome:%s" proteome-id)
                       :fields protein-fields-of-interest}})
        proteins    (-> http-result
                        :body
                        (json/parse-string)
                        (utils/white-space-safe-keywordize-keys)
                        :results)]
    (mapv #(with-meta % uniprotkb-entry-meta) proteins)))

#_(for [proteome '("UP000002438" "UP001223114" "UP001235792" "UP001241625")]
    (count (proteins-by-proteome proteome)))

#_(time (count (proteins-by-proteome "UP000002438")))

(defn ref-proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query (format "organism_id=%s AND proteome_type=\"1\"" taxon-id)
                          #_#_:fields proteome-fields-of-interest})
       (mapv #(with-meta % proteome-meta))))

#_(ref-proteomes-by-taxon-id "208964")

(defn proteomes-by-taxon-id
  [taxon-id]
  (->> (proteomes-search {:query (format "organism_id=%s" taxon-id)
                          #_#_:fields proteome-fields-of-interest})
       (mapv #(with-meta % proteome-meta))))

#_(proteomes-by-taxon-id "83333")

(defn uniref-by-protein-id
  [protein-id]
  (uniref-search {:query (format "uniprot_id:%s" protein-id)}))

#_(map :id (:results (uniref-by-protein-id "A0A0H2ZHP9")))

(defn uniref-entries-by-protein-id
  ([protein-id] (uniref-entries-by-protein-id protein-id #{"UniRef90" "UniRef100"}))
  ([protein-id uniref-cluster-types]
   (let [uniref-clusters (uniref-by-protein-id protein-id)]
     (->> uniref-clusters
          (filter #((set uniref-cluster-types) (:entryType %)))
          (map (comp (fn [[type entry-id]]
                        [type (-> entry-id uniref-entry :members)])
                      (juxt :entryType :id)))
          (into {})))))

#_(uniref-entries-by-protein-id "A0A0H2ZHP9")

(defn uniref-proteins-by-protein-id
  ([protein-id] (uniref-proteins-by-protein-id protein-id
                                               #{"UniRef90" "UniRef100"}))
  ([protein-id uniref-cluster-types]
   (let [cluster (uniref-entries-by-protein-id protein-id
                                               uniref-cluster-types)]
     (->> (for [[cluster-type cluster] cluster]
            (let [{:keys [uni-prot-kb-id uni-parc]}
                  (-> (group-by :memberIdType cluster)
                      (update-keys csk/->kebab-case-keyword))]
              [cluster-type
               {:uniprotkb (->> uni-prot-kb-id
                                (mapcat :accessions)
                                (distinct)
                                (mapv uniprotkb-entry))
                :uniparc   (->> uni-parc
                                (map :memberId)
                                (distinct)
                                (mapv uniparc-entry))}]))
          (into {})))))

#_(uniref-proteins-by-protein-id "A0A0H2ZHP9")

(defn proteins-by-taxa-and-genes
  [taxa genes]
  (let [or-query-part (fn [search-term input]
                        (str "("
                             (str/join " OR "
                                       (map (partial format (str search-term ":%s")) input))
                             ")"))
        gene-query-part (or-query-part "gene" genes)
        taxa-query-part (or-query-part "taxonomy_name" taxa)]
    (uniprotkb-search
     {:query (str gene-query-part " AND " taxa-query-part)})))

#_(count (map :organism (proteins-by-taxa-and-genes ["'Pseudomonas aeruginosa'"] ["mrcA" "mrcB"])))

;;;; Stuff

;; (defn get-inactive-record-via-parc!
;;   [{:keys [extraAttributes]}]
;;   (let [uniparc-record
;;         (http/get
;;          (str "https://rest.uniprot.org/uniparc/"
;;               (:uniParcId extraAttributes)))]
;;     uniparc-record))

;; (defn get-kb-entry-for-parc-entry!
;;   [parc]
;;   (->> parc
;;        :uniParcCrossReferences
;;        (sort-by :lastUpdated)
;;        reverse
;;        (filter #(str/includes? (:database %) "UniProtKB"))
;;        first
;;        :id
;;        (#(http/get
;;           (str "https://www.uniprot.org/uniprotkb/" % ".json")))))

;; (defn get-uniparc-results-for-invalid-uniprot-entries!
;;   [uniprot-records]
;;   (->> uniprot-records
;;        (filter (comp :inactiveReason second))
;;        (map (juxt first
;;                   (comp get-kb-entry-for-parc-entry!
;;                         get-inactive-record-via-parc!
;;                         second)))
;;        (into {})))


(def base-api "https://www.ebi.ac.uk/interpro/api")

(defn get-interpro-entry
  [interpro-accession]
  (-> (http/get (str base-api (format "/entry/interpro/%s" interpro-accession)))
      :body
      (json/parse-string)
      (update-keys keyword)
      (update :metadata #(update-keys % csk/->kebab-case-keyword))))


;; (and (->> (json/parse-string (slurp "json.json") true)
;;           (mapv (comp (fn [accession]
;;                         (let [entry (get-interpro-entry accession)]
;;                           (spit (str "interpro/" accession)
;;                                 entry))) :accession :metadata)))
;;      nil)


(defn flatten-record [record]
  (reduce-kv (fn [m k v]
               (if (map? v)
                 ;; For a nested map, prefix its keys.
                 (merge m (into {} (map (fn [[nk nv]]
                                          [(str (name k) "_" (name nk)) nv])
                                        v)))
                 (assoc m k v)))
             {}
             record))

(def flattened
  (->> (file-utils/ffile-seq (io/file "interpro"))
      (map (comp
            flatten-record
            (fn [f]
              (let [ip (edn/read-string (slurp f))]
                (-> ip :metadata :counters))))))
  )


;; Transform the list of wide maps into a single long-format vector of maps
(def long-format-data
  (->> (mapcat (fn [single-map] ; Process each map in the input list
                 (map (fn [[category-key value]] ; Process each key-value pair within that map
                        {:category (name category-key) ; The key becomes the category identifier
                         :value value})              ; The value is the observed value
                      single-map))
               flattened)
       (filter #(pos-int? (:value %)))))

(def faceted-histogram-spec
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :description "Faceted histograms showing the distribution of values for each category across multiple observations."
   :title "Distribution of Values per Category"
   :data {:values long-format-data} ; Use the transformed long-format data

   ;; Faceting: Create columns based on the 'category' field
   :facet {:column {:field "category"
                    :type "nominal"
                    :title "Category"
                    ;; Optional: Sort the columns/facets if needed
                    ; :sort ["structures" "proteins" "proteomes" ...] ; Explicit order
                    ; :sort {:op "count"} ; Sort by number of non-null values (less useful here)
                    :header {:titleOrient "bottom" :labelOrient "top"} ; Adjust header placement
                   }}

   :spec { ;; Define the plot specification for EACH individual facet
     ;:width 150 ; Adjust width of each individual subplot
     ;:height 150 ; Adjust height of each individual subplot
     :mark "bar" ; Use bars for the histogram

     :encoding {
       :x {:field "value" ; The values within each category go on the X axis
           :type "quantitative"
           ;; Bin the quantitative values to create the histogram bars
           :bin {:maxbins 15} ; Adjust maxbins based on data range and desired granularity
           :title "Value"} ; Title for the X axis within each facet

       :y {:aggregate "count" ; The Y axis shows the frequency (count) of items in each bin
           :type "quantitative"
           :title "Frequency"} ; Title for the Y axis (count of observations)

       :tooltip [ ; Tooltip to show details on hover
         {:field "category" :type "nominal"} ; Show the category for the facet
         {:field "value" :bin true :type "quantitative" :title "Value Range"} ; Show the bin range
         {:aggregate "count" :type "quantitative" :title "Frequency"} ; Show the count in the bin
         ]
       } ; End encoding
     } ; End spec for each facet

   ;; Resolve independent scales for axes if necessary
   ;; Useful if value ranges differ significantly between categories
   :resolve {:scale {:y "independent"  ; Each facet gets its own Y-axis scale
                     :x "independent" ; Uncomment if X-axis ranges also vary wildly
                    }}

   ;; Optional: Configure overall layout
   :config {
       ;:view {:stroke nil} ; Remove outer borders from facets
       :facet {:spacing 15} ; Adjust spacing between facets
       }
   })

(spit "lol.json" (json/generate-string faceted-histogram-spec {:pretty true}))

(defn histogram-spec [field-name values]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :description (str "Histogram of " field-name)
   :data {:values values}
   :mark "bar"
   :encoding {:x {:field field-name
                  :bin true
                  :type "quantitative"}
              :y {:aggregate "count"
                  :type "quantitative"}}})

(def fields-to-plot
  ["structures"
   "domain_architectures"
   "proteomes"
   "taxa"
   "subfamilies"
   "interactions"
   "matches"
   "sets"
   "proteins"
   "pathways"
   "structural_models_alphafold"]) 

(def vega-specs
  (into {}
        (for [field fields-to-plot]
          ;; Extract the data for each field.
          ;; If you need only the field value for each record:
          (let [field-data (map #(select-keys % [field]) flattened)]
            [field (histogram-spec field field-data)]))))

#_(spit
 "proteins.json"
 (json/generate-string (get vega-specs "proteins") {:pretty true}))

(->> flattened
     (map #(get % "proteins"))
     #_(apply max)
     )
