(ns unknown-client.events.db
  (:require
   [re-frame.core :as rf]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def default-db
  {:already-executed-queries #{}
   :forms
   {:msa
    {:params.uniprot/taxonomy
     {:use-taxonomic-search? true}
     :params.uniprot/uniref
     {:use-uniref? true
      :cluster-types #{:uniref-100 :uniref-90}}
     :params.uniprot/blast
     {:use-blast? false}}
    :docking {}}})

;; @re-frame.db/app-db

(rf/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            default-db))
