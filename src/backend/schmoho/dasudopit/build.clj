(ns schmoho.dasudopit.build
  (:require
   [clojure.java.io :as io]
   [fast-edn.core :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.data.codec.base64 :as b64]   [fast-edn.core :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.data.codec.base64 :as b64]
   [schmoho.dasudopit.utils :as utils])
  (:import [java.nio.file Files CopyOption StandardCopyOption]))

(defn copy-recursive [src dest]
  (let [src-dir  (io/file src)
        dest-dir (io/file dest)]
    (doseq [file (.listFiles src-dir)]
      (let [dest-file (io/file dest-dir (.getName file))]
        (if (.isDirectory file)
          (do
            (.mkdirs dest-file)
            (copy-recursive (.getPath file) (.getPath dest-file)))
          (Files/copy (.toPath file)
                      (.toPath dest-file)
                      (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))))))

(defn copy-dist-to-public []
  (copy-recursive "frontend/dist" "resources/public"))

(defn parse-eco-obo
  []
  (->> (utils/read-file "resources/eco.obo")
      (str/split-lines)
      (drop-while #(not (str/starts-with? % "[Term]")))
      (partition-by #(= "[Term]" (str/trim %)))
      (remove #(or (every? str/blank? %)
                   (= "[Term]" (str/trim (first %)))))
      (map (fn [lines]
             (->> (map (fn [line]
                         (map (comp
                               #(str/replace % "\"" "")
                               str/trim) (str/split line #":" 2)))
                       lines)
                  (filter #(#{"id" "name" "def"} (first %)))
                  (reduce
                   (fn [acc [k v]]
                     (assoc acc (keyword k) v))
                   {}))))
      (reduce
       (fn [acc v]
         (assoc acc (:id v) v))
       {})
      (utils/write! "resources/eco.edn")))

