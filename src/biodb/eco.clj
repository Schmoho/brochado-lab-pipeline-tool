(ns biodb.eco
  (:require [utils :as utils]
            [clojure.java.io :as io]))

(def term-lookup
  (utils/read-file (io/resource "eco.edn")))

