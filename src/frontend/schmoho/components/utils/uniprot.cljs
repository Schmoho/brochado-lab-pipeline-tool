(ns schmoho.components.utils.uniprot
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [schmoho.biodb.uniprot.core :as uniprot]
   [schmoho.utils.color :as color]))

(defn- features-with-colors
  [features palette-fn]
  (map
   (fn [domain color]
     (assoc domain :color color))
   (sort-by :location features)
   (palette-fn (count features))))

(defn left-pad [s total-length]
  (gstring/format (str "%0" total-length "s") s))

(defn- location-str
  [feature]
  (let [[start end] (uniprot/feature->location feature)]
    (if (= start end)
      (left-pad (str start) 3)
      (str  (left-pad (str start) 3)
            " - "
            (left-pad (str end) 3)))))

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
        has-afdb?     (uniprot/has-afdb? protein)]
    {:domains       domains
     :active-sites  active-sites
     :binding-sites binding-sites
     :has-afdb?     has-afdb?}))
