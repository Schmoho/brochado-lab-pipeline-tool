(ns schmoho.utils.color
  (:require
   ["chroma-js" :as chroma]))

(defn generate-palette
  "Generates a list of `n` colors interpolated between `start-color` and `end-color`."
  [start-color end-color n]
  (-> (chroma/scale (clj->js [start-color end-color]))
      (.mode "lch")
      (.colors n)))

(def green-yellow-palette (partial generate-palette "#fafa6e" "#2A4858"))
(def red-palette (partial generate-palette "#FFCCCC" "#FF0000"))
(def purple-palette (partial generate-palette "#6A0DAD" "#FF69B4"))

