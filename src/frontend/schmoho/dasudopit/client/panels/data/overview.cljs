(ns schmoho.dasudopit.client.panels.data.overview
  (:require
   [re-com.core :as com]
   [re-frame.core :as rf]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.dasudopit.client.routing :as routing]))

(defn overview-panel []
  (let [volcanos (rf/subscribe [:data/volcanos-list])
        taxons   (rf/subscribe [:data/taxons-list])
        ligands  (rf/subscribe [:data/ligands-list])]
    (fn []
      [structure/collapsible-accordion-2
       ["Volcanos"
        [components.forms/table volcanos
         :columns
         [{:id           :name
           :header-label "Dataset Name"
           :row-label-fn
           (fn [row]
             [com/hyperlink
              :label
              (or (-> row :meta :name not-empty)
                  (-> row :meta :id))
              :on-click
              #(do
                 (rf/dispatch [::forms/set-form-data :volcano-viewer :left :volcano (-> row :meta :id)])
                 (rf/dispatch [::routing/navigate :routing/volcano-viewer]))])}
          {:id           :taxon
           :header-label "Taxon"
           :row-label-fn
           (fn [row]
             [:a {:href (str "taxon/" (-> row :meta :taxon))}
              (-> row :meta :taxon)])}
          {:id :actions
           :header-label "Actions"
           :row-label-fn
           (fn [row]
             [com/row-button
              :md-icon-name "zmdi-delete"
              :mouse-over-row? true
              :on-click #(rf/dispatch [::http/http-delete [:data :volcano (-> row :meta :id)]])])}]]]
       ["Taxons"
        [components.forms/table taxons
         :columns
         [{:id           :id
           :row-label-fn (fn [row]
                           (let [id (-> row :meta :id)]
                             [:a {:href (str "taxon/" id)} id]))
           :header-label "Taxon ID"}
          {:id           :name
           :row-label-fn (comp :name :meta)
           :header-label "Name"}
          {:id :actions
           :header-label "Actions"
           :row-label-fn
           (fn [row]
             [com/row-button
              :md-icon-name "zmdi-delete"
              :mouse-over-row? true
              :on-click #(rf/dispatch [::http/http-delete [:data :taxon (-> row :meta :id)]])])}]]]
       ["Ligands"
        [components.forms/table ligands
         :columns
         [{:id :id
           :row-label-fn (fn [row]
                           (let [id (-> row :meta :cid)]
                             [:a {:href (str "ligand/" id)} id]))
           :header-label "Ligand ID"}
          {:id :name
           :row-label-fn (comp :title :meta)
           :header-label "Name"}
          {:id :actions
           :header-label "Actions"
           :row-label-fn
           (fn [row]
             [com/row-button
              :md-icon-name "zmdi-delete"
              :mouse-over-row? true
              :on-click #(rf/dispatch [::http/http-delete [:data :ligand (-> row :meta :cid)]])])}]]]])))

(defmethod routing/panels :routing.data/overview [] [overview-panel])
(defmethod routing/header :routing.data/overview []
  [structure/header
   :label "Data Overview"])
