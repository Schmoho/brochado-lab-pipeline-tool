(ns schmoho.dasudopit.client.panels.pipelines.docking.part-4
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]))

(defn cutoff-label
  [taxon-id]
  [:span "pLDDT cutoff: " (-> @(rf/subscribe [:forms.docking/input-model])
                              :taxon
                              (get taxon-id)
                              :plddt-cutoff)])

(defn plddt-slider
  [taxon-id]
  (when-let [plddt-viewer @(rf/subscribe [:forms.docking.part-4/plddt-viewer taxon-id])]
    [com/slider
     :model
     (rf/subscribe [:forms.docking.part-4/plddt-cutoff taxon-id])
     :on-change
     #(do
        (doto ^GLViewer @plddt-viewer
          (.setStyle
           (clj->js {})
           (clj->js {:cartoon
                     {:colorfunc
                      (fn [atom]
                        (if (< % (.-b atom))
                          "blue"
                          "yellow"))}}))
          (.render))
        (rf/dispatch [::forms/set-form-data
                      :docking
                      :input-model
                      :taxon
                      taxon-id
                      :plddt-cutoff
                      %]))]))

(defn plddt-viewer
  [protein-data]
  [v
   :children
   [[cutoff-label (:taxon-id protein-data)]
    [plddt-slider (:taxon-id protein-data)]
    [:div
     {:style {:height   "452px"
              :width    "452"
              :position "relative"
              :border   "1px solid black"}}
     [widgets/pdb-viewer
      :pdb (:pdb protein-data)
      :style {:cartoon {:colorfunc
                        (fn [atom]
                          (if (< 80 (.-b atom))
                            "blue"
                            "yellow"))}}
      :config {:backgroundColor "white"}
      :on-load
      #(do
         (rf/dispatch-sync
          [::forms/set-form-data
           :docking
           :input-model
           :taxon
           (:taxon-id protein-data)
           :viewer
           :plddt
           %]))]]]])

(defn part-4
  []
  (let [input-model (rf/subscribe [:forms.docking/input-model])
        structures  (rf/subscribe [:data/structures])]
    (fn []
      (let [viewers (->> @input-model
                         :taxon
                         (map (fn [[taxon-id inputs]]
                                (let [protein-id   (-> inputs :protein :id)
                                      protein-data {:id    protein-id
                                                    :pdb   (:pdb (get @structures protein-id))
                                                    :taxon-id taxon-id
                                                    :viewer (-> inputs)}]
                                  ^{:key protein-id}
                                  [plddt-viewer protein-data]))))]
        [h
         :gap "50px"
         :children
         viewers]))))


