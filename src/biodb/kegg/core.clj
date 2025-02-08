(ns kegg.core
  (:require
   [kegg.api :as kegg.api]
   [kegg.db :as db]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [clojure.java.io :as java.io]
   [clojure.data.csv :as csv]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.shell :as sh]
   [clojure.tools.logging :as log]
   [excel :as excel]
   [clojure.set :as set]
   [clojure.java.io :as io]))


(defn genes
  ([organism] (db/genes organism))
  ([organism genes] (db/genes organism id-list)))

(defn pathways
  ([organism] (db/pathways organism))
  ([organism pathways] (db/pathways organism id-list)))

(defn orthologs [entities] (map db/ortholog entities))

#_(orthologs (take 1 (pathways "pau")))
