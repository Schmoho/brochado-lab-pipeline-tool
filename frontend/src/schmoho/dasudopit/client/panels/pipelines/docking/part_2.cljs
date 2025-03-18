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

(defn handle-get-structures-click-fn
  [selected-proteins selected-taxons]
  (fn []
    (doseq [p selected-proteins]
      (rf/dispatch [::http/http-get [:data :raw :structure p]]))
    (doseq [t selected-taxons]
      (rf/dispatch-sync [::forms/set-form-data
                         :docking
                         :input-model
                         :taxon
                         t
                         :plddt-cutoff
                         80]))))

(defn get-structures-button
  []
  (let [hover? (r/atom false)]
    (fn []
      (let [input-model       @(rf/subscribe [:forms.docking/input-model])
            selected-proteins (->> input-model
                                   :taxon
                                   vals
                                   (map (comp :id :protein)))
            selected-taxons   (->> input-model :taxon keys)]
        (when (every? some? selected-proteins)
          [com/button
           :src       (at)
           :label    "GET STRUCTURES"
           :class    (css/rectangle-button)
           :style    {:background-color "#0072bb"}
           :on-click (handle-get-structures-click-fn selected-proteins selected-taxons)
           :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
           :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                      :on-mouse-out  (com/handler-fn (reset! hover? false))}])))))

(defn taxon-protein-lookup
  [taxon]
  (let [protein-model (rf/subscribe [:forms/by-path :docking :input-model :taxon (:id taxon) :protein])
        protein       @(rf/subscribe [:data/protein (:id @protein-model)])
        form-valid?   @(rf/subscribe [:forms.docking.part-2/valid?])
        proteome      @(rf/subscribe [:data/proteome (:id taxon)])]
    [v
     :children
     [[:h6 (:scientificName taxon)]
      [widgets/protein-search
       :proteome  proteome
       :model     protein-model
       :on-change #(rf/dispatch [::forms/set-form-data
                                 :docking
                                 :input-model
                                 :taxon
                                 (:id taxon)
                                 :protein
                                 %])]
      (when (and protein (not form-valid?))
        [utils/protein-info-hiccup protein])]]))

(defn part-2
  []
  (let [form-valid?        @(rf/subscribe [:forms.docking.part-2/valid?])
        taxon-lookup       @(rf/subscribe [:data/taxons-map])
        selected-taxons    (-> @(rf/subscribe [:forms.docking/input-model]) :taxon keys set)
        proteome-searchers (->> selected-taxons
                                (map taxon-lookup)
                                (map
                                 (fn [taxon]
                                   ^{:key (:id taxon)}
                                   [taxon-protein-lookup taxon])))]
    [v
     :children
     [(when-not form-valid?
        [:span "Please choose a protein for each taxon and press the button to get the structures."])
      [h
       :min-height "300px"
       :gap "30px"
       :children
       (into [] proteome-searchers)]
      [get-structures-button]]]))
