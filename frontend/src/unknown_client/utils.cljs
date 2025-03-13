(ns unknown-client.utils)

(defn cool-select-keys
  [m kaccessors]
  (->> kaccessors
       (map
        (fn [[new-key kaccessor]]
          [new-key (if (coll? kaccessor)
                     (reduce (fn [acc kac]
                               (kac acc))
                             m
                             kaccessor)
                     (kaccessor m))]))
       (into {})))


(defn rand-str
  []
  (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz"))))



;; (def all-global-keys (js->clj (js/Object.getOwnPropertyNames js/window)))

;; (def global-keys (js->clj (js/Object.keys js/window)))
;; (def props (js/Object.getOwnPropertyNames js/$3Dmol))

;; (js->clj props)

;; (vec (for [prop props]
;;    (let [val (aget js/$3Dmol prop)]
;;      [prop (type val)])))
