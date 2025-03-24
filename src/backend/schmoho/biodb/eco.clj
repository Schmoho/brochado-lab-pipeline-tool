(ns schmoho.biodb.eco
  (:require
   [clojure.java.io :as io]
   [schmoho.utils.file :as utils]))

(def term-lookup
  (utils/read-file (io/resource "eco.edn")))

