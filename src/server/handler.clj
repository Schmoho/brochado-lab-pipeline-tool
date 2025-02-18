(ns server.handler
  (:require [graph.accrete.core :as accrete]
            [clojure.tools.logging :as log]))

(defn basic-id-handler
  [type id-accessor]
  (fn 
    [request]
    (let [id          (or (-> request :parameters :query id-accessor)
                          (-> request :parameters :body id-accessor))
          submission  {:id                       id
                       :requested-accretion-type type}
          expectation (promise)]
      (accrete/register-expectation! type id expectation)
      (log/debug "Submitting" submission)
      (accrete/submit! submission)
      {:status 200
       :body   (select-keys
                (deref expectation 10000
                       {:message "Timeout while waiting for confirmation."
                        :type    :confirmation-timeout})
                [:type :id])})))

(def uniprot-taxon-id-handler (basic-id-handler :uniprot/taxon :taxon-id))
(def uniprot-protein-id-handler (basic-id-handler :uniprot/protein :protein-id))
