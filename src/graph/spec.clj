(ns graph.spec
  (:require [clojure.spec.alpha :as s]))

(s/def :cypher/name
  (s/and
   ;; Must start with an alphabetic character or a '$' (for parameter)
   (comp #(re-matches #"^[a-zA-Z\$].*" %) name)
   ;; Should not start with a number
   (comp #(not (re-matches #"^[0-9].*" %)) name)
   ;; Should only contain alphanumeric characters, underscore, or '$' (if not the first character)
   (comp #(re-matches #"^[a-zA-Z\$_][a-zA-Z0-9_\$]*$" %) name)
   ;; Should not exceed 65535 characters
   (comp #(<= (count %) 65535) name)))

(s/def :cypher/ref-id :cypher/name)

(s/def :cypher/labels (s/coll-of keyword?))

(s/def :cypher/value (s/or :string string? :number number?))

(s/def :cypher/props (s/map-of :cypher/name
                               (s/or :value :cypher/value
                                     :value-collection (s/coll-of :cypher/value))))

(s/def :cypher/id string?)

(s/def :cypher/node
  (s/keys :req-un [:cypher/ref-id
                   :cypher/props
                   :cypher/labels]))


(s/def :cypher/from :cypher/node)
(s/def :cypher/to :cypher/node)

(s/def :cypher/rel
  (s/keys :req-un [:cypher/ref-id
                   :cypher/from
                   :cypher/to
                   :cypher/props
                   :cypher/rel-type]))

(s/def :cypher.id/props
  (s/and
   (s/keys :req-un [:cypher/id])
   (s/map-of :cypher/name
             (s/or :value :cypher/value
                   :value-collection (s/coll-of :cypher/value)))))

(s/def :cypher.id/node
  (s/keys :req-un [:cypher/ref-id
                   :cypher/props
                   :cypher/labels]))

(s/def :cypher.id/from :cypher.id/node)
(s/def :cypher.id/to :cypher.id/node)

(s/def :cypher.id/rel
  (s/keys :req-un [:cypher/ref-id
                   :cypher.id/from
                   :cypher.id/to
                   :cypher.id/props
                   :cypher/rel-type]))

(s/def :cypher/nodes (s/coll-of :cypher/node))
(s/def :cypher/rels (s/coll-of :cypher/rel))

(s/def :cypher/graph
  (s/keys :req-un [:cypher/nodes :cypher/rels]))

(s/def :cypher/connection
  some?
  #_(instance? neo4clj/Connection %))
