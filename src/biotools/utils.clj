(ns biotools.utils
  (:require
   [clojure.java.shell :as sh]
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defn is-command-available?
  [cmd]
  (try
    (sh/sh cmd "--help")
       true
       (catch Throwable t
         false)))


