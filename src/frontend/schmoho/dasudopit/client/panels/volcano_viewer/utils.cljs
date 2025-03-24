(ns schmoho.dasudopit.client.panels.volcano-viewer.utils)

;; === GO-Terms ===

(defn protein-go-terms
  [protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(= "GO" (:database %)))
       (map (fn [go-term]
              {:id (:id go-term)
               :label (->> (:properties go-term)
                           (filter #(= "GoTerm" (:key %)))
                           first
                           :value)}))))

(defn proteome-go-terms
  [proteome]
  (->> proteome
      (mapcat protein-go-terms)
      set))

(defn has-go-term?
  [go-term protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(and (= "GO" (:database %))
                     (= go-term (:id %))))
       not-empty))

(defn go-term-filtering-fn
  [proteome go-term]
  (let [proteome-lookup
        (->> proteome
             (map (juxt :primaryAccession identity))
             (into {}))]
    (fn [table-row]
      (some->> table-row
               :protein_id
               proteome-lookup
               (has-go-term? go-term)))))
