(ns unknown-client.db)

(def default-db
  {:name "Unknown Client"
   :already-executed-queries #{}
   :taxonomic-comparison
   {:form
    {:params.uniprot/taxonomy
     {:use-taxonomic-search? true}
     :params.uniprot/uniref
     {:use-uniref? true
      :cluster-types #{:uniref-100 :uniref-90}}
     :params.uniprot/blast
     {:use-blast? false}}}})
