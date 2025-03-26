(ns schmoho.dasudopit.client.panels.pipelines.docking.preprocessing
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.components.pdb :as pdb]))

(defn cutoff-label
  [taxon-id]
  [:span "pLDDT cutoff: "
   (-> @(rf/subscribe [:forms.docking/input-model])
       :taxon
       (get taxon-id)
       :plddt-cutoff)])

(defn handle-set-plddt-cutoff-fn
  [taxon-id]
  (fn [cutoff]
    (rf/dispatch [::forms/set-form-data
                  :docking
                  :input-model
                  :taxon
                  taxon-id
                  :plddt-cutoff
                  cutoff])))

(defn plddt-slider
  [taxon-id]
  (let [plddt-cutoff-model (rf/subscribe [:forms.docking.part-4/plddt-cutoff taxon-id])]
    [com/slider
     :model plddt-cutoff-model
     :on-change (handle-set-plddt-cutoff-fn taxon-id)]))


(defn protein-plddt-cutoff-chooser
  [protein-data]
  (let [taxon-id (:taxon-id protein-data)
        cutoff   @(rf/subscribe [:forms.docking.part-4/plddt-cutoff taxon-id])]
    [v
     :children
     [[cutoff-label taxon-id]
      [plddt-slider taxon-id]
      [pdb/pdb-viewer
       :objects {:pdb (:pdb protein-data)}
       :style {:cartoon {:colorfunc
                         (fn [atom]
                           (if (< (or cutoff 80) (.-b atom))
                             "blue"
                             "yellow"))}}
       :config {:backgroundColor "white"}]]]))

(defn preprocessing-form
  []
  (let [viewers
        (->> @(rf/subscribe [:forms.docking/input-model])
             :taxon
             (map (fn [[taxon-id inputs]]
                    (let [protein-id   (-> inputs :protein :id)
                          protein-data {:id    protein-id
                                        :pdb   (-> @(rf/subscribe [:data/structures])
                                                   (get protein-id)
                                                   :pdb)
                                        :taxon-id taxon-id}]
                      [protein-plddt-cutoff-chooser protein-data]))))]
    [h
     :gap "50px"
     :children
     viewers]))


