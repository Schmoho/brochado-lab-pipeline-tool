(ns schmoho.utils.core)

(defn cool-select-keys
  [m kaccessors]
  (->> kaccessors
       (map
        (fn [[new-key kaccessor]]
          [new-key (if (coll? kaccessor)
                     (reduce (fn [acc kac]
                               (if (string? kac)
                                 (get acc kac)
                                 (kac acc)))
                             m
                             kaccessor)
                     (kaccessor m))]))
       (into {})))
