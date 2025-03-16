(ns schmoho.dasudopit.client.panels.pipelines.docking.part-4
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]))


;; === Subs ===

(rf/reg-sub
 :forms.docking/selected-proteins-model-all
 :<- [:forms/docking]
 (fn
   [form]
   (:selected-proteins-model form)))

;; === Views ===

(defn cutoff-label
  [m]
  [:span "pLDDT cutoff: " @m])


(defn plddt-slider
  [protein-id m]
  [com/slider
   :model
   m
   :on-change
   #(do
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
      (reset! m %))])

(defn part-4
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
                        ^{:key protein-id}
                        [v
                         :children
                         [[cutoff-label m]
                          [plddt-slider protein-id]
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
                            :on-load #(rf/dispatch [::forms/set-form-data
                                                    :docking
                                                    :protein-viewer
                                                    protein-id
                                                    %])]]]]))))
                 (into []))]
        [h
         :gap "50px"
         :children
         viewers]))))


