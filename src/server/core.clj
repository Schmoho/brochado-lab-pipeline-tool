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
   [server.handler :as handler]
   [ring.util.response :as response]
   [ring.middleware.resource :refer [wrap-resource]]
   #_[ring.middleware.content-type :refer [wrap-content-type]]
   [clojure.java.io :as io]))


;; Define a helper to serve your index.html directly
(defn serve-index [req]
  (-> (io/resource "public/index.html")
      slurp
      response/response
      (response/content-type "text/html")))


(defn spa-not-found [req]
  (if (some-> req :headers (get "accept") (str/includes? "text/html"))
    (-> (io/resource "public/index.html")
        slurp
        response/response
        (response/content-type "text/html"))
    {:status 404
     :body "Not Found"}))

(defn redirect-index-html [req]
  (let [uri (:uri req)
        new-uri (str/replace uri #"index\.html$" "")]
    (response/redirect new-uri)))

(def app
  (ring/ring-handler
   (ring/router
    [;; ["/assets/*" {:get (ring/create-resource-handler {:path "/public/assets"})}]
     ;; ["/vendor/*" {:get (ring/create-resource-handler {:path "/public/vendor"})}]
     ;; ["/js/*" {:get (ring/create-resource-handler {:path "/public/js"})}]
     ;; ["/" {:get (fn [_request]
     ;;              {:status 200
     ;;               :headers {"Content-Type" "text/html"}
     ;;               :body (slurp (io/resource "public/index.html"))})}]
     ["/api"
      ["/swagger.json"
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
                :handler handler/get-ligands}}]

        ["/structure/:id"
         {:get {:summary "Get PDB for a UniProt ID."
                :handler handler/get-structure}}]]

       ["/input"
        ["/volcano"
         {:get {:summary "Get volcano data."
                :handler handler/get-volcanos}}]]

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
     ["/" {:get serve-index}]
     ;; Wildcard route to serve index.html for all in-app routes
     #_["/{wildcard:.*index\\.html}"
       {:get redirect-index-html}]
     #_["/*"
      {:get (fn [_]
              (-> (io/resource "public/index.html")
                  slurp
                  ring.util.response/response
                  (ring.util.response/content-type "text/html")))}]]
    {:conflicts (constantly nil)
     ;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
     :validate  spec/validate ;; enable spec validation for route data
     ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
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
                              multipart/multipart-middleware]}})
   (ring/routes
    (ring/create-resource-handler
     {:path "/" })
    (swagger-ui/create-swagger-ui-handler
     {:path   "/"
      :config {:validatorUrl     nil
               :urls             [{:name "swagger" :url "swagger.json"}
                                  {:name "openapi" :url "openapi.json"}]
               :urls.primaryName "openapi"
               :operationsSorter "alpha"}})
    (fn [req]
      (if (some-> req :headers (get "accept") (clojure.string/includes? "text/html"))
        (serve-index req)
        {:status 404 :body "Not Found"}))
   spa-not-found
    (ring/create-default-handler))))

(defn start!
  [port]
  (jetty/run-jetty #'app {:port port, :join? false})
  (log/info (format "HTTP server running on port %s." port)))

(comment
  (def server (start! 3001))
  #_(.stop server))

