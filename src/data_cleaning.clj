(ns data-cleaning
  (:require
   [clojure.edn :as edn]))

(def numerify
  (map
   (fn [m]
     (update-vals m
                  #(let [n (try (edn/read-string %)
                                (catch Exception _))]
                     (if (or (number? n)
                             (nil? n))
                       n
                       %))))))

(defn filtering-insanity
  [rules]
  (filter
   (fn [m]
     (not
      (first
       (filter true?
               (for [[k rule] rules]
                 (rule (get m k)))))))))
