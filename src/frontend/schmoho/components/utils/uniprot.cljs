(ns schmoho.components.utils.uniprot
  (:require
   [schmoho.biodb.uniprot.core :as uniprot]
   [schmoho.utils.color :as color]))

(defn- features-with-colors
  [features palette-fn]
  (map
   (fn [domain color]
     (assoc domain :color color))
   (sort-by :description features)
   (palette-fn (count features))))

(defn- location-str
  [feature]
  (let [[start end] (uniprot/feature->location feature)]
    (if (= start end)
      start
      (str start " - " end))))

(defn protein-info
  [protein]
  (let [features      (:features protein)
        by-type       (->> features
                           (map (fn [f]
                                  (let [type (:type f)]
                                    {:type         (if (#{"Domain"
                                                          "Region"
                                                          "Transmembrane"} type)
                                                     "Domain"
                                                     type)
                                     :description  (:description f)
                                     :location-str (location-str f)
                                     :location     (uniprot/feature->location f)})))
                           (group-by :type))
        domains       (features-with-colors (get by-type "Domain") color/green-yellow-palette)
        active-sites  (features-with-colors (get by-type "Active site") color/red-palette)
        binding-sites (features-with-colors (get by-type "Binding site") color/purple-palette)
        has-afdb?     (->> (:uniProtKBCrossReferences protein)
                           (filter #(= "AlphaFoldDB" (:database %)))
                           seq)]
    {:domains       domains
     :active-sites  active-sites
     :binding-sites binding-sites
     :has-afdb?     has-afdb?}))
