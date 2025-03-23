(ns schmoho.dasudopit.client.common.views.protein
  (:require
   ["chroma-js" :as chroma]
   [schmoho.dasudopit.client.utils :as utils]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]))


(defn generate-palette
  "Generates a list of `n` colors interpolated between `start-color` and `end-color`."
  [start-color end-color n]
  (-> (chroma/scale (clj->js [start-color end-color]))
      (.mode "lch")
      (.colors n)))

(defn features-with-colors
  [features palette-fn]
  (map
   (fn [domain color]
     (assoc domain :color color))
   (sort-by :description features)
   (palette-fn (count features))))

(defn location-str
  [feature]
  (let [[start end] (utils/protein-feature->location feature)]
    (if (= start end)
      start
      (str start " - " end))))

(def green-yellow-palette (partial generate-palette "#fafa6e" "#2A4858"))
(def red-palette (partial generate-palette "#FFCCCC" "#FF0000"))
(def purple-palette (partial generate-palette "#6A0DAD" "#FF69B4"))

(defn protein-info
  [protein]
  (let [features      (:features protein)
        by-type       (->> features
                           (map (fn [f]
                                  (let [type (:type f)]
                                    {:type         (if (#{"Domain"
                                                          "Topological domain"
                                                          "Transmembrane"} type)
                                                     "Domain"
                                                     type)
                                     :description  (:description f)
                                     :location-str (location-str f)
                                     :location     (utils/protein-feature->location f)})))
                           (group-by :type))
        domains       (features-with-colors (get by-type "Domain") green-yellow-palette)
        active-sites  (features-with-colors (get by-type "Active site") red-palette)
        binding-sites (features-with-colors (get by-type "Binding site") purple-palette)
        has-afdb?     (->> (:uniProtKBCrossReferences protein)
                           (filter #(= "AlphaFoldDB" (:database %)))
                           seq)]
    {:domains       domains
     :active-sites  active-sites
     :binding-sites binding-sites
     :has-afdb?     has-afdb?}))

(defn feature->hiccup
  [feature-view-data]
  [h
   :gap "5px"
   :children
   [[:div {:style {:width "10px" :height "10px" :background-color (:color feature-view-data)}}]
    [:span "at residue(s)"]
    [:span (:location-str feature-view-data) ":"]
    [:span (:description feature-view-data)]]])

(defn protein-info-hiccup
  [protein]
  (let [{:keys [has-afdb? domains active-sites binding-sites]}
        (protein-info protein)]
    [v
     :gap "5px"
     :children
     (concat
      [[h
        :gap "5px"
        :children
        [[:span "AlphaFold structure available"]
         (when has-afdb?
           [:i {:class "zmdi zmdi-check"}])]]]
      (into [[:h6 "Domains"]]
            (or (seq (map feature->hiccup domains))
                [[:span "Not available"]]))
      (into [[:h6 "Active Sites"]]
            (or (seq (map feature->hiccup active-sites))
                [[:span "Not available"]]))
      (into [[:h6 "Binding Sites"]]
            (or (seq (map feature->hiccup binding-sites))
                [[:span "Not available"]])))]))
