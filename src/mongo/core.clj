(ns mongo.core
  (:require [monger.core :as mg]
            [monger.db]
            [monger.collection :as mc]
            [clojure.tools.logging :as log])
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId]))

(defn save-api-result!
  [collection stuff]
  (log/debug "Save" stuff "to MongoDB" collection)
  (let [conn   (mg/connect)
        db     (mg/get-db conn "api-cache")
        result (mc/insert-and-return
                db
                collection
                (assoc stuff :_id (ObjectId.)))]
    (mg/disconnect conn)
    result))

(defn get-api-result
  [collection stuff]
  (log/debug "Get" stuff "from MongoDB" collection)
  (let [conn   (mg/connect)
        db     (mg/get-db conn "api-cache")
        result (mc/find-one-as-map db collection stuff)]
    (mg/disconnect conn)
    result))

#_(let [conn (mg/connect)
        db   (mg/get-db conn "api-cache")]
    (monger.db/drop-db db))


#_(let [conn (mg/connect)
      db   (mg/get-db conn "api-cache")]
  (mc/find-maps db "responses"))


#_(mc/find-one-as-map db "documents" { :_id (ObjectId. "4ec2d1a6b55634a935ea4ac8") })


#_(let [conn (mg/connect)
        db   (mg/get-db conn "monger-test")]
    (mc/insert-and-return db "documents" {:name "John" :age 30}))


#_(let [conn (mg/connect)
        db   (mg/get-db conn "monger-test")]
    (mc/insert-batch db "documents" [{:name "Alan" :age 27 :score 17772}
                                     {:name "Joe" :age 32 :score 8277}
                                     {:name "Macy" :age 29 :score 8837777}]))
