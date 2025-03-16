(ns schmoho.dasudopit.client.panels.pipelines.docking.views
  (:require
   ["chroma-js" :as chroma]
   ["chroma-js$default" :as chroma-default]
   [re-frame.core :as rf]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [schmoho.dasudopit.client.common.views.structure :as structure]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-1 :refer [part-1]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-2 :refer [part-2]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-3 :refer [part-3]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-4 :refer [part-4]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-5 :refer [part-5]]
   [schmoho.dasudopit.client.routing :as routing]))


(defn docking-panel
  []
  [structure/collapsible-accordion-2
     ["1. Choose taxons and ligands" [part-1]]
   (when @(rf/subscribe [:forms.docking.part-1/valid?])
     ["2. Choose proteins and binding sites" [part-2]])
   (when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["3. Choose target site" [part-3]])
   (when @(rf/subscribe [:forms.docking.part-2/valid?])
     ["4. Cut tail regions" [part-4]])
     #_["4. Download docking data" [part-4]]
     #_["5. Upload docking results" [part-5]]])

(defmethod routing/panels :routing.pipelines/docking [] [docking-panel])
(defmethod routing/header :routing.pipelines/docking []
  [structure/header :label "Comparative Docking Pipeline"])

