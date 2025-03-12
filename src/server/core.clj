(ns server.core
  (:require
   [clojure.tools.logging :as log]
   [cognitect.transit :as transit]
   [muuntaja.core :as m]
   [reitit.coercion.spec]
   [reitit.dev.pretty :as pretty]
   [reitit.openapi :as openapi]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.ring.spec :as spec]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.adapter.jetty :as jetty]
   [server.handler :as handler]
   [server.spec :as s])
  (:import java.time.Instant))

(def app
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info {:title "unknown-api"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/openapi.json"
      {:get {:no-doc  true
             :openapi {:info {:title       "unknown-api"
                              :description "gives cool"
                              :version     "0.0.1"}}
             :handler (openapi/create-openapi-handler)}}]
     ["/data"
      ["/upload"
       ["/volcano"
        {:post {:summary       "Add a volcano dataset"
                ;; :parameters    {:body :data.input/volcano}
                #_#_:responses {200 {:body :uniprot/basic-response}}
                :handler       handler/upload-volcano}}]]
      ["/raw"
       ["/taxon"
        {:get {:summary "Get taxon data."
               :handler handler/get-taxons}}]
       ["/ligand"
        {:get {:summary "Get ligand data."
               :handler handler/get-ligands}}]]

      ["/input"
       ["/volcano"
        {:get {:summary "Get volcano data."
               :handler handler/get-volcanos}}
        #_["/:id"
           {:get {:summary "Get volcano data."
                  :handler handler/get-volcano}}]]]

      ["/results"
       ["/msa"
        {:get {:summary "Get MSA results."
               :handler handler/get-msa-results-handler}}]]]

     ["/pipelines"
      ["/msa"
       {:post {:summary       "Start taxonomic comparison pipeline."
               ;; :parameters {:body map?}
               #_#_:responses {200 {:body :uniprot/basic-response}}
               :handler       handler/start-msa-handler}}]]]
    {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
     :validate  spec/validate ;; enable spec validation for route data
     ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
     :exception pretty/exception
     :data      {:coercion   reitit.coercion.spec/coercion
                 :muuntaja   m/instance
                 #_(m/create
                    (assoc-in m/default-options
                              [:formats "application/transit+json" :encoder-opts :handlers]
                              {Instant (transit/write-handler "inst" #(.toString %))}))
                 :middleware [;; swagger feature
                              swagger/swagger-feature
                              ;; query-params & form-params
                              parameters/parameters-middleware
                              ;; content-negotiation
                              muuntaja/format-negotiate-middleware
                              ;; encoding response body
                              muuntaja/format-response-middleware
                              ;; exception handling
                              exception/exception-middleware
                              ;; decoding request body
                              muuntaja/format-request-middleware
                              ;; coercing response bodys
                              coercion/coerce-response-middleware
                              ;; coercing request parameters
                              coercion/coerce-request-middleware
                              ;; multipart
                              multipart/multipart-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path   "/"
      :config {:validatorUrl     nil
               :urls             [{:name "swagger" :url "swagger.json"}
                                  {:name "openapi" :url "openapi.json"}]
               :urls.primaryName "openapi"
               :operationsSorter "alpha"}})
    (ring/create-default-handler))))

(defn start!
  [port]
  (jetty/run-jetty #'app {:port port, :join? false})
  (log/info (format "HTTP server running on port %s." port)))

(comment (def server (start! 3001)))

#_(.stop server)

;; ["/accrete"
;;       {:tags ["accrete"]}
;;       ["/uniprot"
;;        ["/taxon"
;;         {:get  {:summary    "Initiate accretion of a taxon by ID."
;;                 :parameters {:query :uniprot/taxon-request}
;;                 :responses  {200 {:body :uniprot/basic-response}}
;;                 :handler    handler/uniprot-taxon-id-handler}
;;          :post {:summary    "Initiate accretion of a taxon by ID."
;;                 :parameters {:body :uniprot/taxon-request}
;;                 :responses  {200 {:body :uniprot/basic-response}}
;;                 :handler    handler/uniprot-taxon-id-handler}}]
;;        ["/protein"
;;         {:get  {:summary    "Initiate accretion of a protein by ID."
;;                 :parameters {:query :uniprot/protein-request}
;;                 :responses  {200 {:body :uniprot/basic-response}}
;;                 :handler    handler/uniprot-protein-id-handler}
;;          :post {:summary    "Initiate accretion of a protein by ID."
;;                 :parameters {:body :uniprot/protein-request}
;;                 :responses  {200 {:body :uniprot/basic-response}}
;;                 :handler    handler/uniprot-protein-id-handler}}]]]
