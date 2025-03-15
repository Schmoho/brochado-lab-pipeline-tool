(ns unknown-client.views.data.data-overview
  (:require
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [unknown-client.routing :as routing]
   [unknown-client.events.routing :as routing-events]
   [unknown-client.events.forms :as form-events]
   [unknown-client.views.common.structure :as structure]
   [unknown-client.views.common.widgets :as widgets]))

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
                (rf/dispatch [::form-events/set-form-data :volcano-viewer :volcano-1 (-> row :meta :id)])
                (rf/dispatch [::routing-events/navigate :routing/volcano-viewer]))])}
          {:id           :taxon
           :header-label "Taxon"
           :row-label-fn
           (fn [row]
             [:a {:href (str "taxon/" (-> row :meta :taxon))}
              (-> row :meta :taxon)])}]]]
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
