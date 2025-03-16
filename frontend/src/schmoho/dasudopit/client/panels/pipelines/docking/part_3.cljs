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

(defn protein-colors
  [protein]
  (let [protein-info (utils/protein-info protein)
        coloring-map (protein-info->coloring-map protein-info)]
    (fn [atom]
      (or (get coloring-map (.-resi atom))
          "grey"))))

(defn part-3
  []
  (let [input-model (rf/subscribe [:forms.docking/input-model])
        structures  (rf/subscribe [:data/structures])]
    (fn []
      (let [protein-ids  (->> @input-model :taxon vals (map (comp :id :protein)))
            protein-data (map
                          (fn [id]
                            {:id      id
                             :pdb     (:pdb (get @structures id))
                             :uniprot @(rf/subscribe [:data/protein id])})
                          protein-ids)
            viewers      (->> protein-data
                              (keep
                               (fn [{:keys [id pdb uniprot]}]
                                 ^{:key id}
                                 [h
                                  :children
                                  [[:div
                                    {:style {:height   "452px"
                                             :width    "452px"
                                             :position "relative"
                                             :border   "1px solid black"}}
                                    [widgets/pdb-viewer
                                     :pdb pdb
                                     :style {:cartoon {:colorfunc (protein-colors uniprot)}}
                                     :config {:backgroundColor "white"}]]
                                   [utils/protein-info-hiccup uniprot]]]))
                              (into []))]
        [h
         :gap "50px"
         :children
         viewers]))))
