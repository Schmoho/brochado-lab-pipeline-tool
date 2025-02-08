(ns db.load.utils
  (:require
   [clojure.string :as str]))

(defn escape-backticks
  [s]
  (when s 
    (str/replace s "'" "\\'")))
