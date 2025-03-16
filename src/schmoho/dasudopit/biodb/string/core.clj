(ns schmoho.dasudopit.biodb.string.core
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]))

(defn id-mapping
  [ids species]
  (let [url (format "https://string-db.org/api/%s/get_string_ids"
                    "json")]
    (-> (http/post url
                   {:form-params {:identifiers (str/join "\r" ids)
                                  :species     species}})
        :body
        (json/parse-string true))))


(defn functional-annotation
  [ids species]
  (let [url (format "https://string-db.org/api/%s/functional_annotation"
                    "json")]
    (let [parts (partition-all 2000 ids)]
      (->> (for [part parts]
             (-> (http/post url
                            {:form-params {:identifiers (str/join "\r" part)
                                           :species     species}})
                 :body
                 (json/parse-string true)))
           (apply concat)))))

(defn network-image
  [ids species image-format]
  (let [url (format "https://string-db.org/api/%s/network"
                    (case image-format
                      :png "highres_image"
                      :svg "svg"))]
    (-> (http/post url
                   {:form-params {:identifiers (str/join "\r" ids)
                                  :species     species}
                    :as :stream})
        :body)))

(defn enrichment-figure
  [ids species category image-format]
  (let [url (format "https://string-db.org/api/%s/enrichmentfigure"
                    (case image-format
                      :png "image"
                      :svg "svg"))]
    (-> (http/post url
                   {:form-params {:identifiers (str/join "\r" ids)
                                  :species     species
                                  :category    category}
                    :as :stream})
        :body)))


(defn enrichment
  ([ids species]
   (let [url (format "https://string-db.org/api/%s/enrichment"
                     "json")]
     (-> (http/post url
                    {:form-params {:identifiers                   (str/join "\r" ids)
                                   :species                       species}})
         :body
         (json/parse-string true))))
  ([ids background species]
   (let [url (format "https://string-db.org/api/%s/enrichment"
                     "json")]
     (-> (http/post url
                    {:form-params {:identifiers                   (str/join "\r" ids)
                                   :background_string_identifiers (str/join "\r" background)
                                   :species                       species}})
         :body
         (json/parse-string true)))))

;; (http/post "https://version-12-0.string-db.org/api/json/enrichment"
;;            {:body (json/generate-string {"identifiers" (str/join "%0d" ["7227.FBpp0074373"])})
;;             :content-type :json})


;; (def string-api-url "https://version-12-0.string-db.org/api")
;; (def output-format "json")
;; (def method "enrichment")


;; (def my-genes ["7227.FBpp0074373" "7227.FBpp0077451" "7227.FBpp0077788"
;;                "7227.FBpp0078993" "7227.FBpp0079060" "7227.FBpp0079448"])

;; (def params {:form-params {:identifiers (clojure.string/join "%0d" my-genes)
;;                            :species 7227
;;                            :caller_identity "www.awesome_app.org"}})

;; (def response (http/post request-url params))

;; (def data (json/parse-string (:body response) true))

;; (doseq [row data]
;;   (let [{:keys [term preferredNames fdr description category]} row]
;;     (when (and (= category "Process") (< fdr 0.01))
;;       ;; Print significant GO Process annotations
;;       (println (clojure.string/join "\t" [term (clojure.string/join "," preferredNames) (str fdr) description])))))
