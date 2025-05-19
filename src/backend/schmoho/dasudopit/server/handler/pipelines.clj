(ns schmoho.dasudopit.server.handler.pipelines
  (:require
   [clojure.tools.logging :as log]
   [schmoho.dasudopit.pipeline.taxonomy :as pipeline.taxonomy]
   [schmoho.utils.file :as utils]
   [schmoho.utils.core :refer [cool-select-keys]]
   [fast-edn.core :as edn]))


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

(defn- target-data
  [docking-data]
  (->> (for [t (:selected-taxons docking-data)]
         [t {:preprocessing
             (cool-select-keys docking-data
                               {:cut?         [:cut? t]
                                :plddt-cutoff [:plddt-cutoffs t]
                                :hydrogenate? [:hydrogenate? t]
                                :charges?     [:charges? t]})
             :structure
             (cool-select-keys docking-data
                               {:protein      [:selected-proteins t :id]
                                :structure    [:selected-structures t]
                                :binding-site [:selected-binding-sites t first]})}])
       (into {})))


{:protein "P02919",
 :name    "MyStruc",
 :taxon   "83333",
 :source  :input, 
 :id      "8ad164d0-a445-41c4-9162-28df3d08a4ce"}

(:selected-ligands  (target-data (edn/read-string (slurp "docking-data.edn"))))

(defn start-docking
  [request]
  (tap> request)
  (let [form (:body-params request)]
    (log/info form)
    {:status 200}))


