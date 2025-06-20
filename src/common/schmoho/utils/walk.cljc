(ns schmoho.utils.walk
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]))

(defn white-space-safe-keywordize-keys
  [m]
  (->> m
       (walk/prewalk (fn [e]
                       (if (and (map-entry? e)
                                (string? (key e)))
                         (update e 0 #(str/replace % #"\s+" ""))
                         e)))
       walk/keywordize-keys))


(defn deep-merge [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))
