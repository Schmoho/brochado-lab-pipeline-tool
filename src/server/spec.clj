(ns server.spec
  (:require
   [clojure.spec.alpha :as s]))

;; (s/def :params.uniprot/blast
;;   (s/keys :requn [:params.uniprot.blast/use-blast?
;;                   :params.uniprot.blast/database
;;                   :params.uniprot.blast/use-blast?]))

;; (s/def :frontend/msa-form
;;   (s/keys :req [:params.uniprot/blast
;;                 :params.uniprot/uniref
;;                 :params.uniprot/taxonomy
;;                 :params.uniprot/protein]))

(s/def :uniprot/id string?)
(s/def :uniprot/type keyword?)

(s/def :uniprot/basic-response (s/keys :req-un [:uniprot/id :uniprot/type]))

(s/def :uniprot/taxon-id some?)
(s/def :uniprot/taxon-request (s/keys :req-un [:uniprot/taxon-id]))

(s/def :uniprot/protein-id some?)
(s/def :uniprot/protein-request (s/keys :req-un [:uniprot/protein-id]))


#_[reitit.ring.middleware.multipart :as multipart]
#_(s/def ::file multipart/temp-file-part)
#_(s/def ::file-params (s/keys :req-un [::file]))
#_(s/def ::name string?)
#_(s/def ::size int?)
#_(s/def ::file-response (s/keys :req-un [::name ::size]))

;; Use data-specs to provide extra JSON-Schema properties:
;; https://github.com/metosin/spec-tools/blob/master/docs/04_json_schema.md#annotated-specs
#_[spec-tools.core :as st]
#_(s/def ::x (st/spec {:spec int?
                     :name "X parameter"
                     :description "Description for X parameter"
                     :json-schema/default 42}))
