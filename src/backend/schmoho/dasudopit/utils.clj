(ns schmoho.dasudopit.utils
  (:require
   [clojure.data.codec.base64 :as b64]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [fast-edn.core :as edn])
  (:import
   (org.apache.commons.io FilenameUtils)))

(defmulti write! (fn [target-file content] (type content)))

(defmethod write! java.lang.String
  [fileo content]
  (when-let [p-dir (.getParentFile (io/file fileo))]
    (.mkdirs p-dir))
  (spit fileo content)
  (io/file fileo))

(defmethod write! :default
  [file content]
  (when-let [p-dir (.getParentFile (io/file file))]
    (.mkdirs p-dir))
  (with-open [wr (io/writer file)]
    (binding [*print-length* nil
              *print-dup* nil
              *print-level* nil
              *print-readably* true
              *out* wr]
      (pr content)))
  (io/file file))

(defn copy!
  [target-file file-to-copy]
  (.mkdirs (.getParentFile (io/file target-file)))
  (io/copy file-to-copy target-file)
  (io/file target-file))

(defmulti extension type)
(defmethod extension java.lang.String
  [file-name]
  (FilenameUtils/getExtension file-name))
(defmethod extension java.io.File
  [file-name]
  (FilenameUtils/getExtension (.getName file-name)))
;; the assumption here is a URL is gonna be a resource
(defmethod extension java.net.URL
  [file]
  (FilenameUtils/getExtension (.getName (io/file file))))

(defmulti base-name type)
(defmethod base-name java.lang.String
  [file-name]
  (FilenameUtils/getBaseName file-name))
(defmethod base-name java.io.File
  [file]
  (FilenameUtils/getBaseName (.getName file)))
;; the assumption here is a URL is gonna be a resource
(defmethod base-name java.net.URL
  [file]
  (FilenameUtils/getBaseName (.getName (io/file file))))

(defmulti read-file extension)
(defmethod read-file "edn"
  [file]
  (edn/read-once (io/file file)))

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

(defn is-command-available?
  [cmd]
  (try
    (sh/sh cmd "--help")
       true
       (catch Throwable t
         false)))

(defn get-hash [x]
  (str/replace (str (clojure.core/hash x)) "-" "0"))


(defn represented-values
  [accessor coll-of-maps]
  (keys (group-by accessor coll-of-maps)))

(defn branching
  [& xforms]
  (mapcat
   (fn branch [e]
     (apply
      concat
      (for [xform (filter some? xforms)]
        (transduce xform conj [] [e]))))))

#_(defn branching [& xforms]
  (fn [rf]
    (let [xrfs (mapv #(% rf) xforms)]
      (fn
        ([acc] (reduce #(%2 %1) acc xrfs))
        ([acc x] (reduce #(%2 %1 x) acc xrfs))))))

(defn encode-base64 [bytes]
  (String. (b64/encode bytes)))

(defn ffile-seq
  [file]
  (->> (file-seq (io/file file))
       (filter #(.isFile %))))

(defn folder-seq
  [file]
  (->> (file-seq (io/file file))
       (filter #(and
                 (not= % (io/file file))
                 (.isDirectory %)))))

;; (defn read-between-markers
;;   [o start-marker end-marker]
;;   (loop [lines   (str/split-lines o)
;;          inside? false
;;          result  []]
;;         (if (empty? lines)
;;           result
;;           (let [line            (first lines)
;;                 remaining-lines (rest lines)]
;;             (cond
;;               (and (not inside?) (= line start-marker))   (recur (rest remaining-lines) true result)
;;               (and inside? (.startsWith line end-marker)) result
;;               inside?                                     (recur remaining-lines inside? (conj result line))
;;               :else                                       (recur remaining-lines inside? result))))))


