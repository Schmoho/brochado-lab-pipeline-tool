(ns biodb.pubchem.core
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def http-get client/get)

#_(-> (client/get "https://pubchem.ncbi.nlm.nih.gov/rest/pug/substance/name/cefotaxim/cids/JSON?list_return=grouped")
      :body
      (json/parse-string true)
      :InformationList :Information
      (->> (map (comp first :CID))
           distinct))
;; => (5479527 6540461 10695961 2632 5742673)

(defn get-compounds
  [nameo]
  (-> (http-get (str "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/"
                     nameo
                     "/JSON"))
      :body
      (json/parse-string true)
      :PC_Compounds))

;; (def compounds (get-compounds "Cefotaxim"))

;; (def compound (-> compounds first))

(def cid-prefix "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/")

(defn cids->titles
  [cids]
  (-> (http-get (str cid-prefix
                (str/join "," cids)
                "/property/Title,MolecularFormula,MolecularWeight,InChIKey/JSON"))
      :body
      (json/parse-string true)
      :PropertyTable
      :Properties))


(def compound-entry->cid
  (comp :cid :id :id))

(defn props
  [compound]
  (->> compound
       :props
       (map (comp :urn))))

(defn cid->2d-sdf
  [cid]
  (-> (http-get (str cid-prefix
                     cid
                     "/SDF"))
      :body))


(defn fetch-and-write-sdfs!
  [nameo path]
  (let [compounds     (get-compounds nameo)
        cids          (->> compounds
                           (map compound-entry->cid))
        filename->sdf (zipmap (->> (cids->titles cids) (map #(str (:Title %) " - " (:CID %) ".sdf")))
                              (map cid->2d-sdf cids))]
    (doseq [[filename sdf] filename->sdf]
      (spit (str path "/" filename) sdf))))

#_(fetch-and-write-sdfs! "cefotaxim" "drugs")
#_(fetch-and-write-sdfs! "amikacin" "drugs")
