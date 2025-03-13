(ns unknown-client.views.pipelines.docking
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]
   [unknown-client.views.common.widgets :as widgets]
   [unknown-client.views.css.forms :as css]
   [unknown-client.events.forms :as form-events]
   [unknown-client.events.http :as http-events]
   [unknown-client.utils :refer [cool-select-keys]]))

(defn docking-header
  []
  [re-com/title
   :src   (at)
   :label "Comparative Docking Pipeline"
   :level :level1])


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
           (take 16)
           (into [])))))

(defn protein-searcher
  [taxon-id]
  (let [proteome @(rf/subscribe [:data/proteome taxon-id])]
    [re-com/typeahead
     :src (at)
     :data-source (protein-search-suggestions proteome)
     :suggestion-to-string #(:protein-name %)
     :render-suggestion
     (fn [{:keys [protein-name gene-name id]}]
       [re-com/hyperlink-href
        :label (str id " - " gene-name " - " protein-name)
        :href (str "/protein/" id)])
     :on-change
     #(rf/dispatch
       [::form-events/set-form-data
        :docking
        :selected-proteins-model
        %])]))

(defn get-structures-button
  []
  (let [hover? (r/atom false)]
    (fn []
      [re-com/button
       :src       (at)
       :label    "GET STRUCTURES"
       :class    (css/rectangle-button)
       :style    {:background-color "#0072bb"}
       :on-click (fn [])
       :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
       :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                  :on-mouse-out  (re-com/handler-fn (reset! hover? false))}])))



(defn docking-panel
  []
  (let [form                  (rf/subscribe [:forms/docking])
        ligands               (rf/subscribe [:data/ligands])
        taxons                (rf/subscribe [:data/taxons])
        taxon-model           (rf/subscribe [:docking/taxon-model])
        internal-taxon-model  (r/atom #{})
        internal-ligand-model (r/atom #{})
        structures            (rf/subscribe [:data/structures])]
    (fn []

      (let [ligand-model            (-> @form :ligand-model)
            taxon-choices           (->> @taxons
                                         (mapv #(cool-select-keys
                                                 %
                                                 [[:label :scientificName]
                                                  [:id :id]])))
            ligand-choices          (->> @ligands
                                         (mapv #(cool-select-keys
                                                 %
                                                 [[:label :name]
                                                  [:id [:json :id :id :cid]]])))
            proteome-searchers
            (->> @taxon-model
                 (mapv
                  (comp
                   (fn [[taxon-id taxon-name]]
                     [:div
                      [:p taxon-name]
                      [protein-searcher taxon-id]])
                   (fn [taxon-id]
                     [taxon-id (:scientificName @(rf/subscribe [:data/taxon taxon-id]))]))))
            selected-proteins-model (-> @form :selected-proteins-model)]
        [v
         :children
         [[h
           :src      (at)
           :children
           [(if (empty? taxon-choices)
              [v
               :justify :center
               :children
               [[re-com/throbber
                 :size :large
                 :style {:width "410px"}]]]
              [re-com/multi-select :src (at)
               :choices       taxon-choices
               :model         internal-taxon-model
               :on-change     #(do
                                 (reset! internal-taxon-model %)
                                 (rf/dispatch
                                  [::form-events/set-form-data
                                   :docking
                                   :taxon-model
                                   %]))
               :width         "450px"
               :left-label    "Available taxons"
               :right-label   "Selected taxons"
               :placeholder   "Select some taxons."
               :required? true
               :filter-box? false])
            [re-com/gap :size "50px"]
            [re-com/multi-select :src (at)
             :choices       ligand-choices
             :model         ligand-model
             :on-change     #(do
                               (reset! internal-taxon-model %)
                               (rf/dispatch
                                [::form-events/set-form-data
                                 :docking
                                 :ligand-model
                                 %]))
             :width         "450px"
             :left-label    "Available ligands"
             :right-label   "Selected ligands"
             :placeholder   "Select at least one ligand."
             :required? true
             :filter-box? false]]]
          [v
           :children
           (into [] proteome-searchers)]
          #_(when (some? selected-proteins-model)
              [v
               :children
               [[get-structures-button]
                [:div
                 {:id                   "bla"
                  :class                "viewer_3Dmoljs"
                  :style                {:height   "400px"
                                         :width    "400px"
                                         :position "relative"}
                ;; :width "600px"
                ;; :height "600px"
                  :data-pdb             "2POR"
                  :data-backgroundcolor "0xffffff"
                  :data-style           "stick"
                  :data-ui              true}]]])
          (when-let [pdb (:pdb (get @structures "A0A0H2ZHP9"))]
            [widgets/pdb-viewer
             :pdb pdb
             :style {:cartoon {:color "spectrum"}}
             :config {:backgroundColor "white"}
             :on-load #(rf/dispatch [::form-events/set-form-data
                                     :docking
                                     :protein-viewer
                                     %])])]]))))

;; (rf/dispatch [::http-events/http-get-2 [:data :raw :structure "A0A0H2ZHP9"]])
;; (:pdb (get @(rf/subscribe [:data/structures]) "A0A0H2ZHP9"))

(let [(-> @re-frame.db/app-db :forms :docking :protein-viewer)]
  )


(defmethod routing/panels :routing.pipelines/docking [] [docking-panel])
(defmethod routing/header :routing.pipelines/docking [] [docking-header])





#_(def v
  (let [element (.querySelector js/document "#bla")
        config {:backgroundColor "orange"}
        viewer (.createViewer js/$3Dmol element (clj->js config))]
    (doto viewer
      (.addModel v (:pdb (get @(rf/subscribe [:data/structures]) "A0A0H2ZHP9")) "pdb")
      (.setStyle v (clj->js {}) (clj->js {:cartoon {:color "spectrum"}}))
      (.zoomTo v)
      (.render v)
      (.zoom v 1.2 1000)
      (.addSphere (clj->js {:center {:x 0 :y 0 :z 0}
                            :radius 10.0
                            :color "green"}))
      (.zoomTo)
      (.render)
      (.zoom 0.8 2000))))


;; (.addModel v (:pdb (get @(rf/subscribe [:data/structures]) "A0A0H2ZHP9")) "pdb")
;; (.setStyle v (clj->js {}) (clj->js {:cartoon {:color "spectrum"}}))
;; (.zoomTo v)
;; (.render v)
;; (.zoom v 1.2 1000)
