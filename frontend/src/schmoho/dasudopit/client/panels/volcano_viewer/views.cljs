(ns schmoho.dasudopit.client.panels.volcano-viewer.views
  (:require
   [schmoho.dasudopit.client.utils :as utils]
   [schmoho.dasudopit.client.vega-utils :as vega-utils]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.forms :as form-views]
   [schmoho.dasudopit.client.common.views.structure :as structure]
   [schmoho.dasudopit.client.panels.volcano-viewer.events :as events]
   [schmoho.dasudopit.client.panels.volcano-viewer.plots :as volcano-plots]
   [schmoho.dasudopit.client.panels.volcano-viewer.subs]
   [schmoho.dasudopit.client.common.views.vega :as vega]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [re-frame.core :as rf]))

(defn data-set-chooser
  [volcano-form volcanos]
  [h
   :src      (at)
   :children [[form-views/dropdown
               :model (-> volcano-form :left :volcano)
               :placeholder "Choose a data set"
               :choices
               (map (fn [v] {:id    (-> v :meta :id)
                             :label (-> v :meta :name)})
                    (vals volcanos))
               :on-change
               #(rf/dispatch [::events/selection :left %])]
              [com/gap :size "1"]
              [form-views/dropdown
               :model (-> volcano-form :right :volcano)
               :placeholder "Choose a data set"
               :choices
               (map (fn [v] {:id    (-> v :meta :id)
                             :label (-> v :meta :name)})
                    (vals volcanos))
               :on-change
               #(rf/dispatch [::events/selection :right %])]]])

(defn selected-genes
  [table]
  [v
   :children
   [[form-views/info-label "Selected genes" [:div ""]]
    [v
     :children
     (into
      []
      (concat (->> @(rf/subscribe [:forms/volcano-viewer])
                   :left
                   :click-selection
                   (map #(vector :span (:gene_name (nth table %)))))
              (->> @(rf/subscribe [:forms/volcano-viewer])
                   :right
                   :click-selection
                   (map #(vector :span (:gene_name (nth table %)))))))]]])

(defn clickable-volcano
  [table]
  [h
   :children
   [[selected-genes table]
    [:div
     {:style {:max-width "800px"}}
     [vega/chart
      {:init-width 800
       :init-height 400
       :spec             (volcano-plots/single-pan :table)
       :data             {:table table}
       :signal-handlers  {"pick" (fn [_signal-name signal-value]
                                   (rf/dispatch [::forms/set-form-data
                                                 :volcano-viewer
                                                 :left
                                                 :click-selection
                                                 (vega-utils/clicked-points-signal signal-value)]))}}]]]])

(defn searchable-volcano
  [table]
  [h
   :children
   [[v
     :children
     [[form-views/info-label
       "Search by gene name"
       [:div ""]]
      [form-views/input-text
       :attr {:id "test"}]]]
    [vega/chart
     {:spec (volcano-plots/single-pan-searchable :table)
      :data {:table table}}]]])

(defn brushed-points
  [table]
  (let [volcano-form (rf/subscribe [:forms/volcano-viewer])]
    [v
     :children
     [[form-views/info-label
       "Selected genes"
       [:div ""]]
      [v
       :children
       (->> table
            (vega-utils/brushed-points
             (-> @volcano-form
                 :left
                 :brush-selection))
            (map #(vector :span %))
            (into []))]]]))

(defn brushable-volcano
  [table]
  [h
   :children
   [[brushed-points table]
    [vega/chart
     {:spec (volcano-plots/single-brush :table)
      :data {:table table}
      :signal-handlers {"brush"
                        (fn [_signal-name signal-value]
                          (rf/dispatch [::forms/set-form-data
                                        :volcano-viewer
                                        :left
                                        :brush-selection
                                        (js->clj signal-value)]))}}]]])

(defn cross-plot
  [table-1 table-2]
  [:div
   {:style {:width  "800px"
            :height "800px"}}
   [vega/chart
    :width "100%"
    :height "100%"
    {:spec
     (volcano-plots/two-volcanoes-cross-plot
      :table
      {:x-label "Cefotaxime Fold Change"
       :y-label "Amikacin Fold Change"})
     :data {:table (vega-utils/cross-data table-1 table-2)}}]])

(defn filterable-volcano
  [table proteome selection]
  (let [go-term-choice (vec (utils/proteome-go-terms proteome))
        filterfn (if (first @selection)
                   (utils/go-term-filtering-fn proteome (first @selection))
                   (constantly true))]
    [h
     :children
     [[h
       :children
       [[com/multi-select :src (at)
         :choices       go-term-choice
         :model         selection
         :on-change     #(rf/dispatch [::forms/set-form-data
                                       :volcano-viewer
                                       :left
                                       :go-filter
                                       %])
         :width         "450px"
         :left-label    "Present GO-terms"
         :right-label   "Selected GO-terms"
         :filter-box? true]
        [:div
         {:style {:width "400px"}}
         [vega/chart
          {:spec (volcano-plots/single-pan :table)
           :data {:table (filter filterfn table)}}]]]]]]))

(defn volcano-panel
  []
  (let [volcanos     (rf/subscribe [:data/volcanos])
        volcano-form (rf/subscribe [:forms/volcano-viewer])]
    (fn []
      (let [volcano-left (get @volcanos (-> @volcano-form :left :volcano))
            volcano-right (get @volcanos (-> @volcano-form :right :volcano))]
        [v
         :children
         [[data-set-chooser @volcano-form @volcanos]
          [structure/collapsible-accordion
           (when-let [table (:table volcano-left)]
             ["Plot Clickable"
              [clickable-volcano table]])
           (when-let [table (:table volcano-left)]
             ["Plot Searchable"
              [searchable-volcano table]])
           (when-let [table (:table volcano-left)]
             ["Plot Brushable"
              [brushable-volcano table]])
           (when-let [table (:table volcano-left)]
             (let [taxon          (-> volcano-left :meta :taxon)
                   proteome       @(rf/subscribe [:data/proteome taxon])]
               ["GO-term filterable"
                [filterable-volcano
                 table
                 proteome
                 (rf/subscribe [:forms.volcano/go-term-selection :left])]]))
           (let [table-1 (:table volcano-left)
                 table-2 (:table volcano-right)]
             (when (and table-1 table-2)
               ["Cross-Plot" [cross-plot table-1 table-2]]))]]]))))

(defmethod routing/panels :routing/volcano-viewer [] [volcano-panel])
(defmethod routing/header :routing/volcano-viewer []
  [structure/header :label "Volcano Viewer"])

#_(defn cross-viz
    [data-set-1
     data-set-2
     cross-data-set
     {:keys [title
             cross-plot-params]}]
    (let [data-set-1 (decorate-data data-set-1)
          data-set-2 (decorate-data data-set-2)
          cross-data (decorate-data cross-data-set)]
      [v
       :children
       [[:h3 title]
        [vega/vega-chart "twocross" (two-volcanoes-cross-plot cross-data cross-plot-params)]
        [vega/vega-chart "st1" (standard data-set-1)]
        [vega/vega-chart "st2" (standard data-set-2)]
        [vega/vega-chart "cross" (cross-highlighting cross-data)]]]))

#_[vega/vega-chart vega/spec]
#_(cross-viz
   data/cefotaxime-ecoli
   data/amikacin-ecoli
   (cross-data data/cefotaxime-ecoli
               data/amikacin-ecoli)
   {:title "Cefotaxime vs. Amikacin E.Coli"
    :cross-plot-params
    {:x-label "Cefotaxime Fold Change"
     :y-label "Amikacin Fold Change"
     :width   500
     :height  500}})
