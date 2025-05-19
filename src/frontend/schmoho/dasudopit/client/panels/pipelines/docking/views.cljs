(ns schmoho.dasudopit.client.panels.pipelines.docking.views
  (:require
   [re-frame.core :as rf]
   [schmoho.components.structure :as structure]
   [schmoho.dasudopit.client.panels.pipelines.docking.provide-data :refer [provide-data-form]]
   [schmoho.dasudopit.client.panels.pipelines.docking.choose-binding-sites :refer [choose-binding-sites-form]]
   [schmoho.dasudopit.client.panels.pipelines.docking.preprocessing :refer [preprocessing-form]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-4 :refer [part-4]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-5 :refer [part-5]]
   [schmoho.dasudopit.client.panels.pipelines.docking.part-6 :refer [part-6]]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.panels.pipelines.docking.subs :as subs]))


(defn docking-panel
  []
  [structure/collapsible-accordion-2
   ["1. Provide and choose data" [provide-data-form]]
   (when @(rf/subscribe [::subs/provided-data-valid?])
     ["2. Preprocessing" [preprocessing-form]])
   (when @(rf/subscribe [::subs/provided-data-valid?])
     ["3. Docking" [choose-binding-sites-form]])
   #_(when @(rf/subscribe [:forms.docking.choose-binding-sites/valid?])
     ["4. Choose target site" [part-4]])
   #_(when @(rf/subscribe [:forms.docking.choose-binding-sites/valid?])
     ["5. Download docking data" [part-5]])
   #_(when @(rf/subscribe [:forms.docking.choose-binding-sites/valid?])
     ["6. Upload docking results" [part-6]])])

(defmethod routing/panels :routing.pipelines/docking [] [docking-panel])
(defmethod routing/header :routing.pipelines/docking []
  [structure/header :label "Comparative Docking Pipeline"])

