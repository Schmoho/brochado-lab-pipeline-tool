(ns biodb.http
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [mongo.core :as mongo]
   [clojure.tools.logging :as log]
   [utils :as utils]))

(defn get
  ([url]
   (get "results" url {}))
  ([url req]
   (get "results" url req))
  ([collection url req]
   (or (-> (mongo/get-api-result
            collection
            {:url     url
             :request req}))
       (let [_      (log/debug "Did not find" url "with" req "in MongoDB" collection)
             result (http/get url req)]
         (mongo/save-api-result!
          collection
          (->  result
               (assoc
                :url     url
                :request req)
               (dissoc :http-client)))))))

(defn id-query
  [url-template result-meta]
  (fn -id-query
      ([id]
       (-id-query id {}))
      ([id query-params]
       (let [url    (format url-template id)
             _      (log/debug "Query" url "with" query-params)
             result (-> (get url {:query-params query-params})
                        (:body)
                        (json/parse-string)
                        (utils/white-space-safe-keywordize-keys))]
        (with-meta result result-meta)))))

