(ns schmoho.dasudopit.client.db
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [schmoho.dasudopit.client.http :as http]))

(def default-db
  {:forms
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

(defn get-data
  [path]
  (when-not (get (@rf.db/app-db :queries) path)
    (rf/dispatch [::http/http-get path])))
