(ns schmoho.utils.csv
  (:require
   #?(:clj [clojure.data.csv :as csv]
      :cljs [goog.labs.format.csv :as csv])
   #?(:clj [clojure.java.io :as io])
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

#?(:clj
   (defn write-csv-data!
     [file csv-data]
     (when-let [p-dir (.getParentFile (io/file file))]
       (.mkdirs p-dir))
     (let [data-to-write (concat [(map name (keys (first csv-data)))]
                                 (map vals csv-data))]
       (with-open [writer (io/writer file)]
         (csv/write-csv writer
                        data-to-write)))))

#?(:clj
   (defn read-csv-data
     [file]
     (-> (io/reader file)
         csv/read-csv
         csv-lines->maps)))

#?(:cljs
   (defn parse-csv-with-header
     [csv-text & {:keys [numeric-fields]}]
     (let [rows   (csv/parse csv-text)      ;; returns a vector of vectors (rows)
           header (first rows)]
       (->> (map #(zipmap header %) (rest rows))
            (mapv (fn [row]
                    (reduce
                     (fn [row field]
                       (update row field #(if (str/includes? % ".")
                                            (parse-double %)
                                            (parse-long %))))
                     row
                     numeric-fields)))))))
