(ns schmoho.components.pdb
  (:require
   ["3dmol/build/3Dmol.js" :as threeDmol]
   ["react" :as react]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [clojure.string :as str]
   [schmoho.components.utils.uniprot :as uniprot]
   [schmoho.components.forms :as forms]
   [reagent.core :as r]))

(defn pdb-upload
  [& {:keys [on-load required? info-body label]
      :or {required? true
           info-body [:<>]}}]
  [v
   :children
   [[forms/info-label
     (if required?
       "Required: Structure File"
       "Optional: Structure File")
     info-body]
    [forms/file-upload
     label
     #(doseq [file (array-seq %)]
        (if-not (str/ends-with? (.-name file) ".pdb")
          (js/alert "Can only handle pdb data.")
          (let [reader (js/FileReader.)]
            (set! (.-onload reader)
                  (fn [_]
                    (let [data (.-result reader)]
                      (on-load (.-name file) data))))
            (.readAsText reader file))))]]])

;; === Structure Viewer ===

(defn pdb-viewer
  [& {:keys [css objects style config on-load structure-reload?]}]
  (let [ref               (react/createRef)
        viewer-state      (r/atom nil)
        current-structure (r/atom nil)]
    (r/create-class
     {:display-name "pdb-viewer"
      :reagent-render
      (fn [& {:keys [objects style]}]
        (when-let [^js viewer @viewer-state]
          (let [{:keys [spheres boxes pdb]} objects]
            (when (and structure-reload? (not= pdb @current-structure))
              (reset! current-structure pdb)
              (doto viewer
                (.removeAllModels)
                (.addModel pdb "pdb")
                (.zoomTo)
                (.zoom 1.2 1000)))
            (.removeAllShapes viewer)
            (doseq [sphere (filter some? spheres)]
              (when (:resi sphere)
                (when-let [^js selected-atoms (first (.selectedAtoms viewer (clj->js {:resi (:resi sphere)})))]
                  (.addSphere viewer (clj->js {:center {:x (.-x selected-atoms),
                                                        :y (.-y selected-atoms)
                                                        :z (.-z selected-atoms)}
                                               :radius (or (:radius sphere) 4.0)
                                               :color  (:color sphere)})))))
            (doseq [box (filter some? boxes)]
              (.addBox viewer (clj->js box)))
            (doto viewer
              (.setStyle (clj->js {}) (clj->js style))
              (.render))))
        [:div
         {:style (merge {:width    "350px"
                         :height   "350px"
                         :position "relative"
                         :border   "solid #cccccc 1px"}
                        css)}
         [:div {:class "mol-container"
                :style {:width    "100%"
                        :height   "100%"
                        :padding  "4px"
                        :position "relative"}
                :ref   ref}
          "Loading viewer..."]])
      :component-did-mount
      (fn [_]
        (when-let [node (.-current ref)]
          (let [^js viewer                  (.createViewer threeDmol node (clj->js config))
                {:keys [pdb spheres boxes]} objects]
            (reset! viewer-state viewer)
            (doseq [sphere (filter some? spheres)]
              (when (:resi sphere)
                (when-let [^js selected-atoms (first (.selectedAtoms viewer (clj->js {:resi (:resi sphere)})))]
                  (.addSphere viewer (clj->js {:center {:x (.-x selected-atoms),
                                                        :y (.-y selected-atoms)
                                                        :z (.-z selected-atoms)}
                                               :radius (or (:radius sphere) 4.0)
                                               :color  (:color sphere)})))))
            (doseq [box (filter some? boxes)]
              (.addBox viewer (clj->js box)))
            (doto viewer
              (.addModel pdb "pdb")
              (.setStyle (clj->js {}) (clj->js style))
              (.zoomTo)
              (.render)
              (.zoom 1.2 1000))
            (reset! current-structure pdb)
            (when on-load
              (on-load viewer-state)))))})))

(defn protein-info->coloring-map
  [protein-info]
  (->> (concat (:domains protein-info)
               (:binding-sites protein-info)
               (:active-sites protein-info))
       (mapcat
        (fn [{:keys [location color]}]
          (zipmap
           (range (first location) (inc (second location)))
           (repeat color))))
       (into {})))

;; === Structural Features Viewer ===

(defn- protein-coloring-fn
  [protein-info]
  (let [coloring-map (protein-info->coloring-map protein-info)]
    (fn [atom]
      (or (get coloring-map (.-resi ^js atom))
          "grey"))))

(defn- protein-info->active-site-balls
  [protein-info]
  (->> protein-info
       :active-sites
       (map (fn [{:keys [location color]}]
              {:resi location :radius 3.0 :color color}))))

(defn structural-features-viewer
  [& {:keys [style pdb uniprot structure-reload?]}]
  (let [protein-info      (uniprot/protein-info uniprot)
        active-site-balls (protein-info->active-site-balls protein-info)
        coloring-fn       (protein-coloring-fn protein-info)]
    [h
     :children
     [[pdb-viewer
       :css style
       :structure-reload? structure-reload?
       :objects {:pdb     pdb
                 :spheres active-site-balls}
       :style {:cartoon {:colorfunc coloring-fn}}
       :config {:backgroundColor "white"}]]]))
