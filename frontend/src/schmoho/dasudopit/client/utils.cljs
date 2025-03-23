(ns schmoho.dasudopit.client.utils
  (:require
   ["chroma-js" :as chroma]
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [schmoho.dasudopit.client.common.http :as http]))

(defn get-data
  [path]
  (when-not (get (@rf.db/app-db :queries) path)
    (rf/dispatch [::http/http-get path])))

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

;; === Color Palettes ===

(defn generate-palette
  "Generates a list of `n` colors interpolated between `start-color` and `end-color`."
  [start-color end-color n]
  (-> (chroma/scale (clj->js [start-color end-color]))
      (.mode "lch")
      (.colors n)))

(def green-yellow-palette (partial generate-palette "#fafa6e" "#2A4858"))
(def red-palette (partial generate-palette "#FFCCCC" "#FF0000"))
(def purple-palette (partial generate-palette "#6A0DAD" "#FF69B4"))





;; (def all-global-keys (js->clj (js/Object.getOwnPropertyNames js/window)))

;; (def global-keys (js->clj (js/Object.keys js/window)))
;; (def props (js/Object.getOwnPropertyNames js/$3Dmol))

;; (js->clj props)

;; (vec (for [prop props]
;;    (let [val (aget js/$3Dmol prop)]
;;      [prop (type val)])))
