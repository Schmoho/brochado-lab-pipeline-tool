(ns db.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [utils :as utils]))

(def db (atom {}))

(defn initialize-db!
  [data-folder]
  (let [tree (->> (file-seq (io/file data-folder))
                  (filter #(.isFile %))
                  (map
                   (comp
                    (fn [path]
                      (let [file-name  (last path)
                            path       (drop 1 (butlast path))
                            dot-ending (str "." (utils/extension file-name))
                            id         (if (str/ends-with? file-name dot-ending)
                                         (subs file-name 0 (- (count file-name)
                                                              (count dot-ending)))
                                         file-name)]
                        [(concat path [id]) file-name]))
                    #(str/split % #"/")
                    #(.getPath %)))
                  (reduce (fn [acc [path file-name]]
                            (assoc-in acc path file-name))
                          {}))]
    (reset! db tree)))

#_(initialize-db! "data")

(defn- fs-pathify
  [path file-type]
  (let [file-type (if (str/starts-with? file-type ".") (subs file-type 1) file-type)]
    (str "data/"
         (->> path (map name) (str/join "/"))
         "."
         file-type)))

(defmulti insert! (fn [path file-type stuff & {:keys [read?]
                                               :or   {read? true}}] (type stuff)))

(defmethod insert! java.io.File
  [path file-type input-file & {:keys [read?]
                                :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/copy! (io/file fs-path) input-file)]
    (swap! db #(assoc-in % path fs-path))
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defmethod insert! :default
  [path file-type stuff & {:keys [read?]
                           :or   {read? true}}]
  (let [fs-path       (fs-pathify path file-type)
        inserted-file (utils/write! (io/file fs-path) stuff)
        path          (map name path)]
    (swap! db #(assoc-in % path fs-path))
    (if read?
      (utils/read-file inserted-file)
      inserted-file)))

(defn db-get
  [path & {:keys [read?]
           :or {read? true}}]
  (some->> (map name path)
           (get-in @db)
           ((if read?
              utils/read-file
              identity))))
