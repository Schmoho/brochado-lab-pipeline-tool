(ns biodb.pubchem
  (:require
   [biodb.http :as http]))

(def cid-prefix "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/")

(defn get-sdf-by-compound-id
  [cid]
  (-> (http/get (str cid-prefix cid "/SDF"))
      :body))

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


;; === Alter IO-Stuff === 


;; (defn fetch-and-write-sdfs!
;;   [nameo path]
;;   (let [compounds     (get-compounds nameo)
;;         cids          (->> compounds
;;                            (map compound-entry->cid))
;;         filename->sdf (zipmap (->> (cids->titles cids) (map #(str (:Title %) " - " (:CID %) ".sdf")))
;;                               (map cid->2d-sdf cids))]
;;     (doseq [[filename sdf] filename->sdf]
;;       (spit (str path "/" filename) sdf))))

#_(fetch-and-write-sdfs! "cefotaxim" "drugs")
#_(fetch-and-write-sdfs! "amikacin" "drugs")
