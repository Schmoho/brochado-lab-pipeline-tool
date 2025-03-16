(ns schmoho.dasudopit.client.panels.pipelines.docking.part-3
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.utils :as utils]))

;; === Subs ===

(rf/reg-sub
 :forms.docking/selected-proteins-model-all
 :<- [:forms/docking]
 (fn
   [form]
   (:selected-proteins-model form)))

;; === Views ===

(defn protein-colors
  [protein]
  (let [features (:features protein)
        by-type  (->> features
                      (map (fn [f]
                             {:type     (:type f)
                              :location (utils/protein-feature->location f)})))])
  (fn [atom]
    "blue"))

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
                    (let [pdb     (:pdb (get @structures protein-id))
                          protein @(rf/subscribe [:data/protein protein-id])]
                      (when pdb
                        ^{:key protein-id}
                        [v
                         :children
                         [[:div
                           {:style {:height   "452px"
                                    :width    "452px"
                                    :position "relative"
                                    :border   "1px solid black"}}
                           [widgets/pdb-viewer
                            :pdb pdb
                            :style {:cartoon {:colorfunc (protein-colors protein)}}
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
