(ns schmoho.dasudopit.biodb.pubchem
  (:require
   [cheshire.core :as json]
   [schmoho.dasudopit.biodb.http :as http]
   [schmoho.dasudopit.utils :as utils]
   [clojure.string :as str]))

(def cid-prefix "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/")
(def name-prefix "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name")
;; === Simple Wrappers ===

(defn get-sdf-by-compound-id
  [cid]
  (-> (http/get (str cid-prefix cid "/SDF"))
      :body))

;; === Convenience Wrappers ===

(defn get-compound-data-by-id
  [cid & {:keys [sdf?]
          :or {sdf? true}}]
  (tap> cid)
  (let [sdf  (when sdf?
               (-> (http/get (str cid-prefix cid "/SDF"))
                   :body))
        json (-> (http/get (str cid-prefix cid "/property/Title/JSON"))
                 :body
                 (json/parse-string true)
                 :PropertyTable
                 :Properties
                 first
                 (update-keys (comp keyword str/lower-case name))
                 (update-vals str))
        png  (-> (http/get (str cid-prefix cid "/PNG?record_type=2d") {:as :byte-array})
                 :body
                 utils/encode-base64)]
    (cond-> {:meta json
             :png  png}
      sdf (assoc :sdf sdf))))


(defn search-compound-by-name
  [name-str]
  (let [cids (-> (http/get (format "%s/%s/JSON"
                                   name-prefix
                                   name-str))
                 :body
                 (json/parse-string true)
                 (->> :PC_Compounds
                      (map (comp :cid :id :id))
                      (take 3)))]
    (->> (for [cid cids]
           [(str cid) (get-compound-data-by-id cid :sdf? false)]))))

(comment

  (-> (get-compound-data-by-id "5742673")
      (update :png utils/encode-base64)
      (->> (utils/write!
            "data/raw/pubchem/compound/5742673.edn")))

;; === Search by name ===

;; (-> (client/get "https://pubchem.ncbi.nlm.nih.gov/rest/pug/substance/name/cefotaxim/cids/JSON?list_return=grouped")
;;       :body
;;       (json/parse-string true)
;;       :InformationList :Information
;;       (->> (map (comp first :CID))
;;            distinct))

;; => (5479527 6540461 10695961 2632 5742673)

;; (defn get-compounds-by-name
;;   [compound-name]
;;   (-> (http/get (str "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/"
;;                      compound-name
;;                      "/JSON"))
;;       :body
;;       (json/parse-string true)
;;       :PC_Compounds))

;; (defn cids->titles
;;     [cids]
;;     (-> (http/get (str cid-prefix
;;                        (str/join "," cids)
;;                        "/property/Title,MolecularFormula,MolecularWeight,InChIKey/JSON"))
;;         :body
;;         (json/parse-string true)
;;         :PropertyTable
;;         :Properties))

;; (cids->titles (map compound-entry->cid compounds))

;; (def compound-entry->cid
;;    (comp :cid :id :id))

 ;; (defn props
 ;;    [compound]
 ;;    (->> compound
 ;;         :props
 ;;         (map (comp :urn))))

;; (def compounds (get-compounds-by-name "Cefotaxim"))

;; (def compound (-> compounds second))

;; => 5742673

;; (-> compound compound-entry->cid get-2d-sdf-by-compound-id)
  )
