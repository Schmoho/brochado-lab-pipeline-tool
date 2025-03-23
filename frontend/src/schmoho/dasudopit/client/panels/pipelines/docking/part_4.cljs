(ns schmoho.dasudopit.client.panels.pipelines.docking.part-4
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs]
   [schmoho.dasudopit.client.common.views.protein :as protein]))

(defn protein-info->coloring-map
  [protein-info]
  (->> (concat (:domains protein-info)
               (:binding-sites protein-info)
               (:active-sites protein-info))
       (mapcat
        (fn [{:keys [location color]}]
          (zipmap
           (range (first location) (inc (second location)))
           (repeat color))))
       (into {})))

(defn protein-coloring-fn
  [protein-info]
  (let [coloring-map (protein-info->coloring-map protein-info)]
    (fn [atom]
      (or (get coloring-map (.-resi ^js atom))
          "grey"))))

(defn protein-site-picker
  [pdb uniprot]
  (let [protein-info (protein/protein-info uniprot)
        active-sites (->> protein-info
                          :active-sites
                          (map (fn [{:keys [location color]}]
                                 {:resi location :radius 3.0 :color color})))]
    [h
     :children
     [[widgets/pdb-viewer
       :objects {:pdb pdb
                 :spheres active-sites}
       :style {:cartoon {:colorfunc (protein-coloring-fn protein-info)}}
       :config {:backgroundColor "white"}]
      #_[com/input-text :model where-box :on-change #(rf/dispatch [::forms/set-form-data
                                                                   :docking
                                                                   :input-model
                                                                   :asdf
                                                                   %])]
      [protein/protein-info->hiccup uniprot]]]))

(defn part-4
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
