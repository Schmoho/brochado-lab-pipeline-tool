(ns unknown-client.db)

(def default-db
  {:name "Unknown Client"
   :taxonomic-comparison
   {:form
    {:uniprot/uniref
     {:cluster-types #{:uniref-100 :uniref-90}}
     :uniprot/blast
     {:database :uniprot-bacteria}}}})
