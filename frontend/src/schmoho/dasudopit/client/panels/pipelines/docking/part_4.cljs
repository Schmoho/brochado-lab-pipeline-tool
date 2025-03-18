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

(defn handle-set-plddt-cutoff-fn
  [taxon-id plddt-viewer]
  (fn [cutoff]
    (rf/dispatch [::forms/set-form-data
                  :docking
                  :input-model
                  :taxon
                  taxon-id
                  :plddt-cutoff
                  cutoff])
    (doto ^GLViewer plddt-viewer
      (.setStyle
       (clj->js {})
       (clj->js {:cartoon
                 {:colorfunc
                  (fn [atom]
                    (if (< cutoff (.-b atom))
                      "blue"
                      "yellow"))}}))
      (.render))))

(defn plddt-slider
  [taxon-id]
  (when-let [plddt-viewer @(rf/subscribe [:forms.docking.part-4/plddt-viewer taxon-id])]
    (let [plddt-cutoff-model (rf/subscribe [:forms.docking.part-4/plddt-cutoff taxon-id])
          viewer             plddt-viewer]
      [com/slider
       :model plddt-cutoff-model
       :on-change (handle-set-plddt-cutoff-fn taxon-id viewer)])))

(defn handle-protein-viewer-on-load-fn
  [taxon-id]
  (fn [viewer]
    (rf/dispatch [::forms/set-form-data
                  :docking
                  :input-model
                  :taxon
                  taxon-id
                  :viewer
                  :plddt
                  @viewer])))

(defn protein-plddt-cutoff-chooser
  [protein-data]
  (let [taxon-id (:taxon-id protein-data)]
    [v
     :children
     [[cutoff-label taxon-id]
      [plddt-slider taxon-id]
      [widgets/pdb-viewer
       :pdb (:pdb protein-data)
       :style {:cartoon {:colorfunc
                         (fn [atom]
                           (if (<  80 (.-b atom))
                             "blue"
                             "yellow"))}}
       :config {:backgroundColor "white"}
       :on-load (handle-protein-viewer-on-load-fn taxon-id)]]]))

(defn part-4
  []
  (let [viewers (->> @(rf/subscribe [:forms.docking/input-model])
                     :taxon
                     (map (fn [[taxon-id inputs]]
                            (let [protein-id   (-> inputs :protein :id)
                                  protein-data {:id    protein-id
                                                :pdb   (:pdb (get @(rf/subscribe [:data/structures]) protein-id))
                                                :taxon-id taxon-id
                                                :viewer (-> inputs)}]
                              [protein-plddt-cutoff-chooser protein-data]))))]
    [h
     :gap "50px"
     :children
     viewers]))


