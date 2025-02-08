(ns utils
  (:require
   [cheshire.core :as json]
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.shell :as sh]))

(defn get-json
  [url]
  (-> (client/get url {:accept :json})
      :body
      (json/parse-string true)))

(defn files-with-ending
  "Path is a string, ending needs to contain the dot."
  [path ending]
  (->> (file-seq (io/file path))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ending))))

(defn keywordize-third-level
  [m]
  (->> m
      (map (fn [[tax prots]]
             [tax (into {}
                        (map (fn [[accession prot]]
                               [accession (walk/keywordize-keys prot)])
                             prots))]))
      (into {})))

(defn is-command-available?
  [cmd]
  (try
    (sh/sh cmd "--help")
       true
       (catch Throwable t
         false)))

(defn read-between-markers
  [o start-marker end-marker]
  (loop [lines   (str/split-lines o)
         inside? false
         result  []]
        (if (empty? lines)
          result
          (let [line            (first lines)
                remaining-lines (rest lines)]
            (cond
              (and (not inside?) (= line start-marker))   (recur (rest remaining-lines) true result)
              (and inside? (.startsWith line end-marker)) result
              inside?                                     (recur remaining-lines inside? (conj result line))
              :else                                       (recur remaining-lines inside? result))))))
