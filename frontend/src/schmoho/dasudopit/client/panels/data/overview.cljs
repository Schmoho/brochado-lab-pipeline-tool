(ns schmoho.dasudopit.client.panels.data.overview
  (:require
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.structure :as structure]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]))

(rf/reg-event-fx
 :testdelete
 (fn [a b]
   (js/alert b)))

(defn overview-panel []
  (let [volcanos (rf/subscribe [:data/volcanos-list])
        taxons   (rf/subscribe [:data/taxons])
        ligands  (rf/subscribe [:data/ligands])]
    (fn []
      [structure/collapsible-accordion-2
       ["Volcanos"
        [widgets/table volcanos
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
              :on-click #(rf/dispatch [:testdelete (-> row :meta :id)])])}]]]
       ["Taxons"
        [widgets/table taxons
         :columns
         [{:id           :id
           :row-label-fn (fn [row]
                           [:a {:href (str "taxon/" (:id row))}
                            (:id row)])
           :header-label "Taxon ID"}
          {:id           :scientificName
           :header-label "Name"}]]]
       ["Ligands"
        [widgets/table ligands
         :columns
         [{:id :id
           :row-label-fn (fn [row]
                           [:a {:href (str "ligand/" (:id row))}
                            (:id row)])
           :header-label "Ligand ID"}
          {:id :name
           :header-label "Name"}]]]])))

(defmethod routing/panels :routing.data/overview [] [overview-panel])
(defmethod routing/header :routing.data/overview []
  [structure/header
   :label "Data Overview"])
