(ns schmoho.dasudopit.server.handler.pipelines
  (:require
   [clojure.tools.logging :as log]
   [schmoho.dasudopit.pipeline.taxonomy :as pipeline.taxonomy]
   [schmoho.utils.file :as utils]))


(defn start-msa-handler
  [request]
  (tap> request)
  (let [uuid (random-uuid)
        form (-> request :body-params (assoc :pipeline/uuid uuid))]
    (log/info "Run job with UUID" uuid)
    (log/info "Write params.")
    (utils/write!
     (format "data/results/msa/%s/params.edn" (str uuid)))
    (future (try
              (pipeline.taxonomy/pipeline form)
              (catch Throwable t
                (log/error t))))
    {:status 200
     :body   {:job-id uuid}}))


