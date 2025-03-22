(ns schmoho.dasudopit.server.handler.results
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [fast-edn.core :as edn]
   [schmoho.dasudopit.pipeline.taxonomy :as pipeline.taxonomy]))

;; TODO: das ganze Zeug hier sollte über DB API laufen!
(defn get-msa-results-handler
  [request]
  (log/info "Getting taxonomic comparison results.")
  (let [results (->> (file-seq (io/file "data/results/msa"))
                     (filter #(and (not= % (io/file "data/results/msa"))
                                   (.isFile %)
                                   (str/ends-with? (.getName %) ".edn")))
                     (mapv #(edn/read-once (io/file %)))
                     (mapv (fn [result]
                             (if (@pipeline.taxonomy/running-blast-jobs
                                  (:pipeline/uuid result))
                               (assoc result :blast-still-running? true)
                               result))))]
    {:status 200
     :body   {:results results}}))

