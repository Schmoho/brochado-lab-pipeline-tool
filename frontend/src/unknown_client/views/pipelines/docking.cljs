(ns unknown-client.views.pipelines.docking
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]
   [unknown-client.subs.forms.docking]
   [unknown-client.views.common.forms :as forms]
   [unknown-client.views.common.structure :as structure]
   [unknown-client.views.common.widgets :as widgets]
   [unknown-client.views.css.forms :as css]
   [unknown-client.views.vega :as vega]
   [unknown-client.events.forms :as form-events]
   [unknown-client.events.http :as http-events]
   [unknown-client.utils :refer [cool-select-keys] :as utils]))


(defn taxon-choice
  [& {:keys [taxons on-change model]}]
  (let [taxon-choices
            (->> taxons
                 (mapv #(cool-select-keys
                         %
                         [[:label :scientificName]
                          [:id :id]])))]
        (if (empty? taxons)
          [v
           :justify :center
           :children
           [[com/throbber
             :size :large
             :style {:width "410px"}]]]
          [com/multi-select :src (at)
           :choices       taxon-choices
           :model model
           :on-change     #(when on-change
                             (on-change %))
           :width         "450px"
           :left-label    "Available taxons"
           :right-label   "Selected taxons"
           :placeholder   "Select some taxons."
           :required? true
           :filter-box? false])))

(defn ligand-choice
  [& {:keys [ligands on-change model]}]
  (let [ligand-choices
        (->> ligands
             (mapv #(cool-select-keys
                     %
                     [[:label :name]
                      [:id [:json :id :id :cid]]])))]
    [com/multi-select :src (at)
     :choices       ligand-choices
     :model         model
     :on-change     #(when on-change
                       (on-change %))
     :width         "450px"
     :left-label    "Available ligands"
     :right-label   "Selected ligands"
     :placeholder   "Select at least one ligand."
     :required? true
     :filter-box? false]))

(defn part-1
  []
  [v
   :width "200px"
   :children
   [[h
     :src      (at)
     :children
     [[taxon-choice
       :taxons
       @(rf/subscribe [:data/taxons])
       :model
       (rf/subscribe [:forms.docking/taxon-model])
       :on-change
       #(do
          (doseq [chosen-taxon %]
            (utils/get-data [:data :raw :proteome chosen-taxon]))
          (rf/dispatch
           [::form-events/set-form-data
            :docking
            :taxon-model
            %]))]
      [com/gap :size "50px"]
      [ligand-choice
       :ligands
       @(rf/subscribe [:data/ligands])
       :model
       (rf/subscribe [:forms.docking/ligand-model])
       :on-change
       #(rf/dispatch
         [::form-events/set-form-data
          :docking
          :ligand-model
          %])]]]]])


(defn get-structures-button
  []
  (let [selected-proteins (rf/subscribe [:forms.docking/selected-proteins-ids])
        selected-taxa (rf/subscribe [:forms.docking/taxon-model])
        hover?            (r/atom false)]
    (fn []
      (when (and (every? some? @selected-proteins)
                 (seq @selected-proteins)
                 (= (count @selected-taxa) (count @selected-proteins)))
        [com/button
         :src       (at)
         :label    "GET STRUCTURES"
         :class    (css/rectangle-button)
         :style    {:background-color "#0072bb"}
         :on-click (fn []
                     (doseq [p @selected-proteins]
                       (rf/dispatch [::http-events/http-get [:data :raw :structure p]])))
         :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
         :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                    :on-mouse-out  (com/handler-fn (reset! hover? false))}]))))

(defn location->str
  [location]
  (let [start (-> location :start :value)
        end (-> location :end :value)]
    (if (= start end)
      start
      (str start " - " end))))

(defn feature->hiccup
  [feature]
  [v
   :children
   [[h
     :gap "5px"
     :children
     [[:b (:type feature)]
      "at"
      [:span (location->str (:location feature))]]]
    [:span (:description feature)]]])

(defn protein->has-afdb-hiccup
  [protein]
  (when (->> (:uniProtKBCrossReferences protein)
             (filter #(= "AlphaFoldDB" (:database %)))
             seq)
    [h :gap "5px" :children [[:span "AlphaFold"] [:i {:class "zmdi zmdi-check"}]]]))



(defn part-2
  []
  (let [taxons @(rf/subscribe [:forms.docking/taxon-model-resolved])
        proteome-searchers
        (->> taxons
             (mapv
              (fn [taxon]
                [v
                 :children
                 [[:h6 (:scientificName taxon)]
                  [widgets/protein-search
                   :proteome
                   @(rf/subscribe [:data/proteome (:id taxon)])
                   :model
                   (rf/subscribe [:forms.docking/selected-protein-for-taxon (:id taxon)])
                   :on-change
                   #(rf/dispatch [::form-events/set-form-data
                                  :docking
                                  :selected-proteins-model
                                  (:id taxon)
                                  %])]
                  (let [protein @(rf/subscribe [:forms.docking/selected-protein-for-taxon-resolved (:id taxon)])]
                    [v
                     :gap "5px"
                     :children
                     (into
                      [(protein->has-afdb-hiccup protein)]
                      (->> (:features protein)
                           (filter #(#{"Active site" "Binding site"} (:type %)))
                           (map feature->hiccup)))])]])))]
    [v
     :children
     [[h
       :min-height "300px"
       :gap "30px"
       :children
       (into [] proteome-searchers)]
      [get-structures-button]]]))

(defn cutoff-label
  [m]
  [:span "pLDDT cutoff: " @m])

(defn part-3
  []
  (let [selected-proteins (rf/subscribe [:forms.docking/selected-proteins-model-all])
        structures        (rf/subscribe [:data/structures])]
    (fn []
      (let [viewers
            (->> @selected-proteins
                 vals
                 (map :id)
                 (keep
                  (fn [protein-id]
                    (when-let [pdb (:pdb (get @structures protein-id))]
                      (let [m (r/atom 80.0)]
                        [v
                         :children
                         [[cutoff-label m]
                          [com/slider
                           :model
                           m
                           :on-change #(do
                                         (let [viewer (-> @re-frame.db/app-db :forms :docking :protein-viewer first val deref)]
                                           (doto  ^GLViewer viewer
                                             (.setStyle
                                              (clj->js {})
                                              (clj->js {:cartoon
                                                        {:colorfunc
                                                         (fn [atom]
                                                           (if (< % (.-b atom))
                                                             "blue"
                                                             "yellow"))}}))
                                             (.render)))
                                         (reset! m %))]
                          [:div
                           {:style {:height "452px"
                                    :width "452"
                                    :position "relative"
                                    :border "1px solid black"}}
                           [widgets/pdb-viewer
                            :pdb pdb
                            :style {:cartoon {:colorfunc
                                              (fn [atom]
                                                (if (< @m (.-b atom))
                                                  "blue"
                                                  "yellow"))}}
                            :config {:backgroundColor "white"}
                            :on-load #(rf/dispatch [::form-events/set-form-data
                                                    :docking
                                                    :protein-viewer
                                                    protein-id
                                                    %])]]]]))))
                 (into []))]
        [h
         :gap "50px"
         :children
         viewers]))))

(defn part-4
  [proteins]
  [v
   :children
   [[:h4 {:style {:color "darkred"}} "Dummy button for illustration"]
    [:div [forms/action-button
           :label "Download"]]]])


(defn part-5
  [proteins]
  [v
   :children
   [[:h4 {:style {:color "darkred"}} "Dummy data for illustration"]
    [:div
     [forms/action-button
      :label "Upload"]]
    [h
     :children
     [[vega/vega-chart
       :id "omse"
       :spec
       {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
        :description "A simple bar chart with embedded data."
        :data {:values [{:a "A" :b 28}
                        {:a "B" :b 55}
                        {:a "C" :b 43}
                        {:a "D" :b 91}
                        {:a "E" :b 81}
                        {:a "F" :b 53}
                        {:a "G" :b 19}
                        {:a "H" :b 87}
                        {:a "I" :b 52}]}
        :mark "bar"
        :encoding {:x {:field "a" :type "nominal" :axis {:labelAngle 0}}
                   :y {:field "b" :type "quantitative"}}}]
      [:img {:src "https://www.ebi.ac.uk/thornton-srv/software/LigPlus/manual2/ligplot_3tmn.gif"
             :style {:width "300px"
                     :height "auto"}}]]]]])

(defn docking-panel
  []
  [structure/collapsible-accordion-2
   ["1. Choose taxons and ligands" [part-1]]
   ["2. Choose proteins and binding sites" [part-2]]
   ["3. Cut tail regions" [part-3]]
   ["4. Download docking data" [part-4]]
   ["5. Upload docking results" [part-5]]])

(defmethod routing/panels :routing.pipelines/docking [] [docking-panel])
(defmethod routing/header :routing.pipelines/docking []
  [structure/header :label "Comparative Docking Pipeline"])

