(ns schmoho.dasudopit.client.panels.pipelines.docking.part-4
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs]))

(defn part-4
  []
  [:p "hi"]
  #_(let [input-model     (rf/subscribe [:forms.docking/input-model])
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
