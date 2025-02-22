(ns biotools.utils
  (:require [clojure.java.shell :as sh]))


(defn is-command-available?
  [cmd]
  (try
    (sh/sh cmd "--help")
       true
       (catch Throwable t
         false)))
