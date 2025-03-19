(ns schmoho.dasudopit.client.panels.pipelines.docking.part-3
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs]
   [schmoho.dasudopit.client.panels.pipelines.docking.utils :as utils]))

(defn protein-info->coloring-map
  [protein-info]
  (into
   {}
   (mapcat
    (fn [{:keys [location color]}]
      (zipmap
       (range (first location) (inc (second location)))
       (repeat color)))
    (concat (:domains protein-info)
            (:binding-sites protein-info)
            (:active-sites protein-info)))))

(defn protein-coloring-fn
  [protein]
  (let [protein-info (utils/protein-info protein)
        coloring-map (protein-info->coloring-map protein-info)]
    (fn [atom]
      (or (get coloring-map (.-resi atom))
          "grey"))))

(rf/reg-sub
 :where-box
 :<- [:forms.docking/input-model]
 (fn [input-model]
   (-> input-model :asdf)))

(defn protein-site-picker
  [pdb uniprot]
  (let [where-box (rf/subscribe [:where-box])]
    (.log js/console @where-box)
    [h
     :children
     [[widgets/pdb-viewer
       :objects {:pdb pdb
                 :spheres [(when (= "10" @where-box)
                             {:center {:x 0, :y 0, :z 0}, :radius 10.0, :color "green"})]}
       :style {:cartoon {:colorfunc (protein-coloring-fn uniprot)}}
       :config {:backgroundColor "white"}]
      [com/input-text :model where-box :on-change #(rf/dispatch [::forms/set-form-data
                                                                 :docking
                                                                 :input-model
                                                                 :asdf
                                                                 %])]
      [utils/protein-info-hiccup uniprot]]]))

(defn part-3
  []
  (let [input-model     (rf/subscribe [:forms.docking/input-model])
        structures      (rf/subscribe [:data/structures])
        protein-viewers (->> @input-model
                             :taxon
                             vals
                             (map
                              (comp
                               (fn [id]
                                 (let [pdb     (:pdb (get @structures id))
                                       uniprot @(rf/subscribe [:data/protein id])]
                                   [protein-site-picker pdb uniprot]))
                               :id
                               :protein)))]
    [h
     :gap "50px"
     :children
     protein-viewers]))
