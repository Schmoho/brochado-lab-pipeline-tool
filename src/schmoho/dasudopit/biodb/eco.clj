(ns schmoho.dasudopit.biodb.eco
  (:require
   [clojure.java.io :as io]
   [schmoho.dasudopit.utils :as utils]))

(def term-lookup
  (utils/read-file (io/resource "eco.edn")))

