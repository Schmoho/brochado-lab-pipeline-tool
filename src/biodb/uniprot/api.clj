(ns uniprot.api
  (:require
   [clojure.string :as str]
   [biodb.http :as http]
   [cheshire.core :as json]
   [utils :refer [get-json]]))

;; (def get-uniprotkb-record!
;;   (memoize
;;    (fn [url-or-id]
;;      (if (str/includes? url-or-id "https://rest.uniprot.org/uniprotkb/")
;;        (get-json url-or-id)
;;        (get-json (str "https://rest.uniprot.org/uniprotkb/" url-or-id))))))


;; (defn get-inactive-record-via-parc!
;;   [{:keys [extraAttributes]}]
;;   (let [uniparc-record
;;         (get-json
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
;;        (#(get-json
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
