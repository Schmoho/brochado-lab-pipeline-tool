(ns utils
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.set :as set])
  (:import
   (org.apache.commons.io FilenameUtils)))

(defmulti write! (fn [target-file content] (type content)))

(defmethod write! java.lang.String
  [fileo content]
  (.mkdirs (.getParentFile (io/file fileo)))
  (spit fileo content)
  (io/file fileo))

(defmethod write! :default
  [fileo content]
  (.mkdirs (.getParentFile (io/file fileo)))
  (with-open [wr (io/writer fileo)]
    (.write wr (with-out-str (pprint/pprint content))))
  (io/file fileo))

(defn copy!
  [target-file file-to-copy]
  (.mkdirs (.getParentFile (io/file target-file)))
  (io/copy file-to-copy target-file)
  (io/file target-file))

(defn extension
  [file-name]
  (FilenameUtils/getExtension file-name))

(defmulti read-file (fn [file] (FilenameUtils/getExtension (.getName (io/file file)))))

(defmethod read-file "edn"
  [file]
  (tap> file)
  (edn/read (java.io.PushbackReader. (io/reader file))))

(defmethod read-file :default
  [file]
  (slurp file))

(defn create-temp-file
  [file-ending]
  (let [temp-file (java.io.File/createTempFile "unknown-"
                                               (if (str/starts-with? file-ending ".")
                                                 file-ending
                                                 (str "." file-ending)))]
    ;; Ensure the file is deleted when the JVM exits:
    (.deleteOnExit temp-file)
    temp-file))

(defn white-space-safe-keywordize-keys
  [m]
  (->> m
       (walk/prewalk (fn [e]
                       (if (and (map-entry? e)
                                (string? (key e)))
                         (update e 0 #(str/replace % #"\s+" ""))
                         e)))
       walk/keywordize-keys))

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
