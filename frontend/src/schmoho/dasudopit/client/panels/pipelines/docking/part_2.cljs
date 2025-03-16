(ns schmoho.dasudopit.client.panels.pipelines.docking.part-2
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.http :as http]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.css.forms :as css]
   [schmoho.dasudopit.client.panels.pipelines.docking.utils :as utils]))

(defn get-structures-button
  []
  (let [input-model (rf/subscribe [:forms.docking/input-model])
        hover?      (r/atom false)]
    (fn []
      (let [selected-proteins (->> @input-model :taxon vals (map (comp :id :protein)))]
        (when (every? some? selected-proteins)
          [com/button
           :src       (at)
           :label    "GET STRUCTURES"
           :class    (css/rectangle-button)
           :style    {:background-color "#0072bb"}
           :on-click (fn []
                       (doseq [p selected-proteins]
                         (rf/dispatch [::http/http-get [:data :raw :structure p]]))
                       (doseq [t (->> @input-model :taxon keys)]
                         (rf/dispatch-sync [::forms/set-form-data
                                            :docking
                                            :input-model
                                            :taxon
                                            t
                                            :plddt-cutoff
                                            80])))
           :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
           :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                      :on-mouse-out  (com/handler-fn (reset! hover? false))}])))))




(defn hint
  []
  (when-not @(rf/subscribe [:forms.docking.part-2/valid?])
    [:span "Please choose a protein for each taxon and press the button to get the structures."]))


(defn part-2
  []
  (let [taxon-lookup @(rf/subscribe [:data/taxons-map])
        input-model  (-> @(rf/subscribe [:forms.docking/input-model]) :taxon keys set)
        proteome-searchers
        (->> input-model
             (map taxon-lookup)
             (mapv
              (fn [taxon]
                (let [protein-model (rf/subscribe [:forms/by-path :docking :input-model :taxon (:id taxon) :protein])]
                  ^{:key (:id taxon)}
                  [v
                   :children
                   [[:h6 (:scientificName taxon)]
                    [widgets/protein-search
                     :proteome  @(rf/subscribe [:data/proteome (:id taxon)])
                     :model     protein-model
                     :on-change #(rf/dispatch [::forms/set-form-data
                                               :docking
                                               :input-model
                                               :taxon
                                               (:id taxon)
                                               :protein
                                               %])]
                    (let [protein @(rf/subscribe [:data/protein (:id @protein-model)])]
                      (when (and protein (not @(rf/subscribe [:forms.docking.part-2/valid?]))) 
                        [utils/protein-info-hiccup protein]))]]))))]
    [v
     :children
     [[hint]
      [h
       :min-height "300px"
       :gap "30px"
       :children
       (into [] proteome-searchers)]
      [get-structures-button]]]))
