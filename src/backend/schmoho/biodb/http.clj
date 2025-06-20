(ns schmoho.biodb.http
  (:refer-clojure :exclude [get])
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [schmoho.utils.walk :as utils]))

(defn get
  ([url]
   (get "results" url {}))
  ([url req]
   (get "results" url req))
  ([collection url req]
   (or #_(-> (mongo/get-api-result
            collection
            {:url     url
             :request req}))
       (let [_      (log/debug "Did not find" url "with" req "in MongoDB" collection)
             result (http/get url req)]
         result
         #_(mongo/save-api-result!
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
     (let [url          (format url-template id)
           query-params (update query-params :fields #(if (coll? %)
                                                        (str/join "," %)
                                                        %))
           _            (log/debug "Query" url "with" query-params)
           result       (-> (get url {:query-params query-params})
                            (:body)
                            (json/parse-string)
                            (utils/white-space-safe-keywordize-keys))]
       (with-meta result result-meta)))))

