(ns schmoho.dasudopit.client.panels.pipelines.docking.views
  (:require
   [re-frame.core :as rf]
   [schmoho.components.structure :as structure]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-1 :refer [part-1]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-2 :refer [part-2]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-3 :refer [part-3]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-4 :refer [part-4]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-5 :refer [part-5]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-6 :refer [part-6]]
   [schmoho.dasudopit.client.routing :as routing]))


(defn docking-panel
  []
  [structure/collapsible-accordion-2
   ["1. Choose taxons and ligands" [part-1]]
   (when @(rf/subscribe [:forms.docking.part-1/valid?])
     ["2. Choose proteins and binding sites" [part-2]])
   #_(when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["3. Preprocessing" [part-3]])
   #_(when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["4. Choose target site" [part-4]])
   #_(when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["5. Download docking data" [part-5]])
   #_(when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["6. Upload docking results" [part-6]])])

(defmethod routing/panels :routing.pipelines/docking [] [docking-panel])
(defmethod routing/header :routing.pipelines/docking []
  [structure/header :label "Comparative Docking Pipeline"])

