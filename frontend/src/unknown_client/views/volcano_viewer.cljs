(ns unknown-client.views.volcano-viewer
  (:require
   [unknown-client.utils :as utils]
   [unknown-client.vega-utils :as vega-utils]
   [unknown-client.routing :as routing]
   [unknown-client.events.forms :as form-events]
   [unknown-client.events.vega :as vega-events]
   [unknown-client.views.common.forms :as forms]
   [unknown-client.views.common.structure :as structure]
   [unknown-client.subs.forms.volcano-viewer]
   [unknown-client.views.plots.volcano :as volcano-plots]
   [unknown-client.views.vega :as vega]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [re-frame.core :as rf]))

(defn data-set-chooser
  [volcano-form volcanos]
  [h
   :src      (at)
   :children [[forms/dropdown
               :model (:volcano-1 volcano-form)
               :placeholder "Choose a data set"
               :choices
               (map (fn [v] {:id    (-> v :meta :id)
                             :label (-> v :meta :name)})
                    (vals volcanos))
               :on-change
               #(rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-1 %])]
              [com/gap :size "1"]
              [forms/dropdown
               :placeholder "Choose a data set"
               :choices
               (map (fn [v] {:id    (-> v :meta :id)
                             :label (-> v :meta :name)})
                    (vals volcanos))
               :on-change
               #(rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-2 %])]]])

(defn clickable-volcano
  [table]
  (let [volcano-form (rf/subscribe [:forms/volcano-viewer])]
    (fn []
      [h
       :children
       [[vega/vega-chart
         :spec (volcano-plots/single-pan table)
         :id "v1"
         :on-change
         #(do
            (rf/dispatch
             [::vega-events/register-signal-listener
              %
              {:signal-name
               "pick"
               :listener-fn
               (fn [_signal-name signal-value]
                 (rf/dispatch [::form-events/set-form-data
                               :volcano-viewer
                               :volcano-1-selection
                               (vega-utils/clicked-points-signal signal-value)]))}])
            (rf/dispatch [::form-events/set-form-data
                          :volcano-viewer
                          :volcano-1-view
                          %]))]
        [v
         :children
         [[forms/info-label "Selected genes" [:div ""]]
          [v
           :children
           (into
            []
            (->> @volcano-form
                 :volcano-1-selection
                 (map #(vector :span (:gene_name (nth table %))))))]]]]])))

(defn searchable-volcano
  [table]
  [h
   :children
   [[vega/vega-chart
     :spec (volcano-plots/single-pan-searchable table)
     :id "v1_2"]
    [v
     :children
     [[forms/info-label
       "Search by gene name"
       [:div ""]]
      [forms/input-text
       :attr {:id "test"}]]]]])

(defn brushable-volcano
  [table]
  (let  [volcano-form (rf/subscribe [:forms/volcano-viewer])]
    (fn []
      [h
       :children
       [[vega/vega-chart
         :spec (volcano-plots/single-brush table)
         :id "v2"
         :on-change
         #(do
            (rf/dispatch [::vega-events/register-signal-listener
                          %
                          {:signal-name
                           "brush"
                           :listener-fn
                           (fn [_signal-name signal-value]
                             (rf/dispatch [::form-events/set-form-data
                                           :volcano-viewer
                                           :volcano-3-selection
                                           (js->clj signal-value)]))}])
            (rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-3-view %]))]
        [v
         :children
         [[forms/info-label
           "Selected genes"
           [:div ""]]
          [v
           :children
           (->> table
                (vega-utils/brushed-points (:volcano-3-selection @volcano-form))
                (map #(vector :span %))
                (into []))]]]]])))

(defn cross-plot
  [table-1 table-2]
  [:div
   {:style {:width  "800px"
            :height "800px"}}
   [vega/vega-chart
    :width "100%"
    :height "100%"
    :spec
    (volcano-plots/two-volcanoes-cross-plot
     (vega-utils/cross-data table-1 table-2)
     {:x-label "Cefotaxime Fold Change"
      :y-label "Amikacin Fold Change"
      :width   600
      :height  600})
    :id "v3"]])

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
       [[vega/vega-chart
         :spec (volcano-plots/single-pan (filter filterfn table))
         :id "v8"]
        [com/multi-select :src (at)
         :choices       go-term-choice
         :model         selection
         :on-change     #(rf/dispatch [::form-events/set-form-data :volcano-viewer :go-filter %])
         :width         "450px"
         :left-label    "Present GO-terms"
         :right-label   "Selected GO-terms"
         :filter-box? true]]]]]))

(defn volcano-panel
  []
  (let [volcanos     (rf/subscribe [:data/volcanos])
        volcano-form (rf/subscribe [:forms/volcano-viewer])]
    (fn []
      (let [volcano-1 (get @volcanos (:volcano-1 @volcano-form))
            volcano-2 (get @volcanos (:volcano-2 @volcano-form))
            taxon      (-> volcano-1 :meta :taxon)
            proteome   @(rf/subscribe [:data/proteome taxon])]
        [v
         :children
         [[data-set-chooser @volcano-form @volcanos]
          [structure/collapsible-accordion
           (when-let [table (:table volcano-1)]
               ["Plot Clickable"
                [clickable-volcano table]])
           (when-let [table (:table volcano-1)]
               ["Plot Searchable"
                [searchable-volcano table]])
           (when-let [table (:table volcano-1)]
               ["Plot Brushable"
                [brushable-volcano table]])
           (when-let [table (:table volcano-1)]
             (let [taxon          (-> volcano-1 :meta :taxon)
                   proteome       @(rf/subscribe [:data/proteome taxon])]
               ["GO-term filterable"
                [filterable-volcano table proteome (rf/subscribe [:forms.volcano/go-term-selection])]]))
           (let [table-1 (:table volcano-1)
                 table-2 (:table volcano-2)]
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
