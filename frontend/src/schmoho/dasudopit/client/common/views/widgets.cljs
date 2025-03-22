(ns schmoho.dasudopit.client.common.views.widgets
  (:require
   ["react" :as react]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [reagent.core :as r]
   [schmoho.dasudopit.client.utils :refer [rand-str]]
   ["3dmol/build/3Dmol.js" :as threeDmol]))

(defn lineage-item [item]
  [v
   :align :center
   :class "btn btn-outline-secondary"
   :children [#_[com/label :label (:rank item)]
              [com/hyperlink-href :label (:scientificName item)
               :href (str "https://www.uniprot.org/taxonomy/" (:id item))]]])

(defn lineage-connector []
  [:div {:style {:display        "flex"
                 :flex-direction "column"
                 :align-items    "center"
                 :margin         "3px 0 0 0"}}
   [:div {:style {:width            "8px"
                  :height           "8px"
                  :background-color "#ccc"
                  :border-radius    "50%"}}]
   [:div {:style {:width            "2px"
                  :height           "10px"
                  :background-color "#ccc"}}]])

(defn lineage-widget [lineage]
  [v
   :children (if (empty? lineage)
               [[com/label :label "No lineage available"]]
               (->> lineage
                    (map lineage-item)
                    (interpose [lineage-connector])
                    reverse
                    vec))])

(defn protein-search-suggestions
  [proteome]
  (fn [s]
    (let [fragment (re-pattern (str "(?i)" (or s "")))]
      (->> proteome
           (keep (fn [protein]
                   (let [gene-name    (or (-> protein :genes first :geneName :value)
                                          (-> protein :genes first :orderedLocusNames :value)
                                          "-")
                         protein-name (or (-> protein :proteinDescription :recommendedName :fullName :value)
                                          (-> protein :proteinDescription :submissionNames first :fullName :value)
                                          "-")
                         protein-id   (or (-> protein :primaryAccession)
                                          "-")]
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (take 6)
           (into [{:id nil :protein-name "-" :gene-name nil}])))))

(defn protein-search
  [& {:keys [proteome on-change model]}]
  (let [suggestions-for-search (protein-search-suggestions proteome)]
    [v
     :src      (at)
     :children
     [[h
       :src (at)
       :children
       [[:span.field-label "Search Proteins"]
        [com/info-button
         :src (at)
         :info
         [v
          :src (at)
          :children
          [[:p.info-heading "Organism ID"]
           [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
           [com/hyperlink-href :src (at)
            :label  "Link to docs."
            :href   ""
            :target "_blank"]]]]]]
      [com/typeahead
       :src (at)
       :model model
       :placeholder (str (:protein-name @model))
       :rigid? true
       :change-on-blur? true
       :on-change
       #(when on-change
          (on-change %))
       :data-source suggestions-for-search
       :suggestion-to-string #(:protein-name %)
       :render-suggestion
       (fn [{:keys [protein-name gene-name id]}]
         [:span (str id " - " gene-name " - " protein-name)])
       :parts
       {:suggestions-container {:style {:z-index 10}}
        :suggestion {:style {:z-index 10}}}]]]))

(defn pdb-viewer
  [& {:keys [objects style config on-load]}]
  (let [ref          (react/createRef)
        viewer-state (r/atom nil)]
    (r/create-class
     {:display-name "pdb-viewer"
      :reagent-render
      (fn [& {:keys [objects style]}]
        (when-let [^js viewer @viewer-state]
          (let [{:keys [spheres boxes]} objects]
            (.removeAllShapes viewer)
            (doseq [sphere (filter some? spheres)]
              #_(.addSphere viewer (clj->js sphere))
              (when (:resi sphere)
                (when-let [^js selected-atoms (first (.selectedAtoms viewer (clj->js {:resi (:resi sphere)})))]
                  (.addSphere viewer (clj->js {:center {:x (.-x selected-atoms),
                                                        :y (.-y selected-atoms)
                                                        :z (.-z selected-atoms)}
                                               :radius (or (:radius sphere) 4.0)
                                               :color (:color sphere)})))))
            (doseq [box (filter some? boxes)]
              (.addBox viewer (clj->js box)))
            (doto viewer
              (.setStyle (clj->js {}) (clj->js style))
              (.render))))
        [:div {:class "mol-container"
               :style {:width    "450px"
                       :height   "450px"
                       :position "relative"
                       :border   "solid grey 1px"}
               :ref   ref}
         "Loading viewer..."])
      :component-did-mount
      (fn [_]
        (when-let [node (.-current ref)]
          (let [^js viewer (.createViewer threeDmol node (clj->js config))
                {:keys [pdb spheres boxes]} objects]
            (reset! viewer-state viewer)
            #_(doseq [sphere (filter some? spheres)]
                (.addSphere viewer (clj->js sphere)))
            (doseq [sphere (filter some? spheres)]
              #_(.addSphere viewer (clj->js sphere))
              (when (:resi sphere)
                (when-let [^js selected-atoms (first (.selectedAtoms viewer (clj->js {:resi (:resi sphere)})))]
                  (.addSphere viewer (clj->js {:center {:x (.-x selected-atoms),
                                                        :y (.-y selected-atoms)
                                                        :z (.-z selected-atoms)}
                                               :radius (or (:radius sphere) 4.0)
                                               :color (:color sphere)})))
                #_(.addSphere viewer (clj->js sphere))))
            (doseq [box (filter some? boxes)]
              (.addBox viewer (clj->js box)))
            (doto viewer
              (.addModel pdb "pdb")
              (.setStyle (clj->js {}) (clj->js style))
              (.zoomTo)
              (.render)
              (.zoom 1.2 1000))
            (when on-load
              (on-load viewer-state)))))})))

(defn table
  [data & {:keys [columns on-enter-row on-leave-row]}]
  (if (nil? @data)
    [com/throbber :size :regular]
    [v
     :width "1550px"
     :max-width "1550px"
     :children
     [[h
       :children
       [[com/simple-v-table
         :src                       (at)
         :model data
         :max-width "1000px"
         :columns
         (mapv (fn [defaults input]
                 (merge defaults input))
               (map (fn [col]
                      (assoc
                       {:width 300
                        :align "center"
                        :vertical-align "middle"}
                       :row-label-fn #((:id col) %)
                       :header-label (name (:id col))))
                    columns)
               columns)
         :row-height                35
         :on-enter-row on-enter-row
         :on-leave-row on-leave-row]]]]]))

(defn alert
  [& {:keys [heading body dismissible? alert-type]}]
  [:div {:class (str "alert fade show "
                     (case alert-type
                       :warning "alert-warning "
                       :danger "alert-danger "
                       :success "alert-success "
                       :primary "alert-primary "
                       :secondary "alert-secondary "
                       :info "alert-info "
                       "alert-info ")
                     (when dismissible? "alert-dismissible "))
         :role "alert"}
   [:strong heading]
   body
   [:button {:type "button"
             :class "close"
             :data-dismiss "alert"
             :aria-label "Close"}
    [:span {:aria-hidden "true"} "×"]]])
