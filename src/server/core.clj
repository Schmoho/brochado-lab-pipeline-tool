(ns server.core
  (:require
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
   [server.handler.data :as data-handler]
   [server.handler.pipelines :as pipelines-handler]
   #_[ring.middleware.reload :refer [wrap-reload]]
   [ring.util.response :as response]
   [clojure.java.io :as io]))


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
       ["/raw"
        
        ["/ligand"
         {:get {:summary "Get ligand data."
                :handler #'data-handler/get-ligands}}]

        ["/proteome/:id"
         {:get {:summary "Get proteome for a UniProt taxon ID."
                :handler #'data-handler/get-proteome}}]

        ["/structure/:id"
         {:get {:summary "Get PDB for a UniProt ID."
                :handler #'data-handler/get-structure}}]

        ["/taxon"
         {:get {:summary "Get taxon data."
                :handler #'data-handler/get-taxons}}]]

       ["/input"
        ["/volcano"
         {:get {:summary "Get volcano data."
                :handler #'data-handler/get-volcanos}}]]

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
     {:path "/" })
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

