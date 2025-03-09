(ns csv-utils
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn csv-lines->maps
  [csv-lines & {:keys [keywordize?] :or {keywordize? true}}]
  (map zipmap
       (->> (first csv-lines)
            (map
             (comp
              (if keywordize?
                keyword
                identity)
              #(str/replace % "." "_")))
            repeat)
       (rest csv-lines)))

(defn field-domain
  [field csv-data]
  (set (map field csv-data)))

(defn write-csv-data!
  [file csv-data]
  (when-let [p-dir (.getParentFile (io/file file))]
    (.mkdirs p-dir))
  (let [data-to-write (concat [(map name (keys (first csv-data)))]
                              (map vals csv-data))]
    (with-open [writer (io/writer file)]
      (csv/write-csv writer
                     data-to-write))))

(defn read-csv-data
  [file]
  (-> (io/reader file)
      csv/read-csv
      csv-utils/csv-lines->maps))
