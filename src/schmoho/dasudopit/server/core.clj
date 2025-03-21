(ns schmoho.dasudopit.server.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
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
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.util.response :as response]
   [schmoho.dasudopit.server.handler.data :as data-handler]
   [schmoho.dasudopit.server.handler.pipelines :as pipelines-handler]
   [clojure.walk :as walk]))

;; Define a helper to serve your index.html directly
(defn serve-index [req]
  (-> (io/resource "public/index.html")
      slurp
      response/response
      (response/content-type "text/html")))

(def app
  (ring/ring-handler
   (ring/router
    [["/api"
      ["/swagger.json"
       {:get {:no-doc  true
              :swagger {:info {:title "unknown-api"}}
              :handler (swagger/create-swagger-handler)}}]
      ["/openapi.json"
       {:get {:no-doc  true
              :openapi {:info {:title       "pipeline webtool API"
                               :description "API for the pipeline webtool"
                               :version     "0.0.1"}}
              :handler (openapi/create-openapi-handler)}}]
      ["/data"
       ["/upload"
        ["/volcano"
         {:post {:summary       "Add a volcano dataset"
                 ;; using vars (#') allows to update the handler
                 ;; and have the changes immediately be reflected in a REPL
                 :handler       #'data-handler/upload-volcano}}]]
       ["/structures/:protein-id"
        {:get {:summary "Get all structures (AFDB, input and processed) for a UniProt protein ID."
                :handler #'data-handler/get-all-structures-for-protein-id}}]
       ["/raw"

        ["/ligand"
         {:get {:summary "Get ligand data."
                :handler #'data-handler/get-ligands}}]

        ["/ligand/:id"
         {:get {:summary "Get ligand data by Pubchem ID."
                :handler #'data-handler/get-ligand}}]
        
        ["/proteome/:id"
         {:get {:summary "Get proteome for a UniProt taxon ID."
                :handler #'data-handler/get-proteome}}]

        ["/structure/:id"
         {:get {:summary "Get AFDB PDB for a UniProt ID."
                :handler #'data-handler/get-afdb-structure}}]

        ["/taxon"
         {:get {:summary "Get taxon data."
                :handler #'data-handler/get-taxons}}]
        
        ["/taxon/:id"
         {:get {:summary "Get taxon data by NCBI/UniProt Taxonomy ID."
                :handler #'data-handler/get-taxon}}]]

       ["/input"
        ["/structure/:id"
         {:get {:summary "Get user input PDB for UUID."
                :handler #'data-handler/get-input-structure}}]
        
        ["/volcano"
         {:get {:summary "Get volcano data."
                :handler #'data-handler/get-volcanos}}]]

       ["/processed"
        ["/structure/:id"
         {:get {:summary "Get processed PDB for UUID."
                :handler #'data-handler/get-processed-structure}}]]

       ["/results"
        ["/msa"
         {:get {:summary "Get MSA results."
                :handler #'data-handler/get-msa-results-handler}}]]]

      ["/pipelines"
       ["/msa"
        {:post {:summary       "Start taxonomic comparison pipeline."
               ;; :parameters {:body map?}
                #_#_:responses {200 {:body :uniprot/basic-response}}
                :handler       #'pipelines-handler/start-msa-handler}}]]]
     ["/" {:get serve-index}]]
    {:conflicts (constantly nil)
     ;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
     :validate  spec/validate ;; enable spec validation for route data
     :exception pretty/exception
     :data      {:coercion   reitit.coercion.spec/coercion
                 :muuntaja   m/instance
                 #_(m/create
                    (assoc-in m/default-options
                              [:formats "application/transit+json" :encoder-opts :handlers]
                              {Instant (transit/write-handler "inst" #(.toString %))}))
                 :middleware [swagger/swagger-feature
                              #(wrap-cors % :access-control-allow-origin [#".*"]
                                          :access-control-allow-methods [:get :put :post :delete])
                              ;; query-params & form-params
                              parameters/parameters-middleware
                              muuntaja/format-negotiate-middleware
                              muuntaja/format-response-middleware
                              exception/exception-middleware
                              muuntaja/format-request-middleware
                              coercion/coerce-response-middleware
                              coercion/coerce-request-middleware
                              multipart/multipart-middleware
                              ;; dev-time s.t. changed namespaces are reloaded
                              #_wrap-reload]}})
   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (swagger-ui/create-swagger-ui-handler
     {:path   "/api/openapi"
      :config {:validatorUrl     nil
               :urls             [{:name "swagger" :url "/api/swagger.json"}
                                  {:name "openapi" :url "/api/openapi.json"}]
               :urls.primaryName "openapi"
               :operationsSorter "alpha"}})
    (fn [req]
      (if (some-> req :headers (get "accept") (clojure.string/includes? "text/html"))
        (serve-index req)
        {:status 404 :body "Not Found"}))
    (ring/create-default-handler))))

(defn start!
  [port]
  (jetty/run-jetty #'app {:port port, :join? false})
  (log/info (format "HTTP server running on port %s." port)))

(comment
  (def server (start! 3001))
  (.stop server))
