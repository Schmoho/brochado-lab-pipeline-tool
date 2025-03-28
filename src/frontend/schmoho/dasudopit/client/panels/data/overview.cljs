(ns schmoho.dasudopit.client.panels.data.overview
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.dasudopit.client.routing :as routing]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.components.pdb :as pdb]
   [clojure.walk :as walk]
   [schmoho.components.uniprot :as uniprot]
   [schmoho.components.pubchem :as pubchem]))

(def form-model
  {:active-preview [:data/overview :active-preview]
   :preview-type   [:data/overview :preview-type]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(defn protein-preview-modal
  []
  (let [active-preview @(model :active-preview)
        preview-type   @(model :preview-type)]
    (when (and active-preview
               (= preview-type :protein))
      [com/modal-panel
       :backdrop-on-click #((setter :active-preview) nil)
       :child
       (let [active-protein    @(rf/subscribe [:data/protein (:protein active-preview)])
             protein-structure @(rf/subscribe [:data/structure-by-type
                                               (:protein active-preview)
                                               (name (:source active-preview))
                                               (:id active-preview)])]
         (if-not active-protein
           [com/throbber]
           [h
            :children
            [[pdb/structural-features-viewer
              :pdb (:structure protein-structure)
              :uniprot active-protein]
             [uniprot/protein-structural-features-overview active-protein]]]))])))

(defn ligand-preview-modal
  []
  (let [active-preview @(model :active-preview)
        preview-type   @(model :preview-type)]
    (when (and active-preview
               (= preview-type :ligand))
      (let [ligand @(rf/subscribe [:data/ligand (-> active-preview :meta :cid)])]
        [com/modal-panel
         :backdrop-on-click #((setter :active-preview) nil)
         :child
         [pubchem/ligand-viewer ligand]]))))

(defn overview-panel []
  [:<>
   [protein-preview-modal]
   [ligand-preview-modal]
   (let [structures     (rf/subscribe [:data/structures-list])
         volcanos       (rf/subscribe [:data/volcanos-list])
         taxons         (rf/subscribe [:data/taxons-list])
         ligands        (rf/subscribe [:data/ligands-list])]
     [structure/collapsible-accordion-2
      ["Structures"
       [components.forms/table structures
        :columns
        [{:id           :name
          :header-label "Name"
          :row-label-fn :name}
         {:id           :protein
          :header-label "Uniprot ID"
          :row-label-fn
          (fn [row]
            (let [id (:protein row)]
              [:a {:href   (str "https://www.uniprot.org/uniprotkb/" id)
                   :target "_blank"} id]))}
         {:id           :taxon
          :header-label "Taxon"
          :row-label-fn
          (fn [row]
            (let [id (:taxon row)]
              [:a {:href   (str "https://www.uniprot.org/taxonomy/" id)
                   :target "_blank"} id]))}
         {:id           :source
          :header-label "Source"
          :row-label-fn
          (fn [row]
            (case (:source row)
              :input     "User Input"
              :processed "Automatically preprocessed"
              "Unknown"))}
         {:id           :id
          :header-label "Show Structure"
          :row-label-fn
          (fn [row]
            [com/md-icon-button
             :md-icon-name "zmdi-open-in-new"
             :size :smaller
             :on-click
             #(do
                (db/get-data [:data :taxon (:taxon row) :proteome])
                (db/get-data [:data
                              :structure
                              (:protein row)
                              (-> row :source name)
                              (:id row)])
                ((setter :active-preview) row)
                ((setter :preview-type) :protein))])}
         {:id           :actions
            :header-label "Actions"
            :row-label-fn
            (fn [row]
              [com/row-button
               :md-icon-name "zmdi-delete"
               :mouse-over-row? true
               :on-click #(rf/dispatch [::http/http-delete [:data :structure (:protein row) :input (:id row)]])])}]]]
      ["Volcanos"
       [components.forms/table volcanos
        :columns
        [{:id           :name
          :header-label "Dataset Name"
          :row-label-fn
          (fn [row]
            [com/hyperlink
             :style {:color "#007bff"}
             :label
             (or (-> row :meta :name not-empty)
                 (-> row :meta :id))
             :on-click
             #(do
                (rf/dispatch [::forms/set-form-data :volcano-viewer :left :volcano (-> row :meta :id)])
                (db/get-data [:data :volcano (-> row :meta :id)])
                (rf/dispatch [::routing/navigate :routing/volcano-viewer]))])}
         {:id           :taxon
          :header-label "Taxon"
          :row-label-fn
          (fn [row]
            (let [id (-> row :meta :taxon)]
              [:a {:href   (str "https://www.uniprot.org/taxonomy/" id)
                   :target "_blank"} id]))}
         {:id           :actions
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
                            [:a {:href   (str "https://www.uniprot.org/taxonomy/" id)
                                 :target "_blank"} id]))
          :header-label "Taxon ID"}
         {:id           :name
          :row-label-fn (comp :name :meta)
          :header-label "Name"}
         {:id           :actions
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
        [{:id           :id
          :row-label-fn (fn [row]
                          (let [id (-> row :meta :cid)]
                            [:a {:href (str "ligand/" id)} id]))
          :header-label "Ligand ID"}
         {:id           :name
          :row-label-fn (comp :title :meta)
          :header-label "Name"}
         {:id           :actions
          :header-label "Actions"
          :row-label-fn
          (fn [row]
            [com/row-button
             :md-icon-name "zmdi-delete"
             :mouse-over-row? true
             :on-click #(rf/dispatch [::http/http-delete [:data :ligand (-> row :meta :cid)]])])}
         {:id           :id
          :header-label "Show Structure"
          :row-label-fn
          (fn [row]
            [com/md-icon-button
             :md-icon-name "zmdi-open-in-new"
             :size :smaller
             :on-click
             #(do
                (db/get-data [:data :ligand (-> row :meta :cid)])
                ((setter :active-preview) row)
                ((setter :preview-type) :ligand))])}]]]])])



(defmethod routing/panels :routing.data/overview [] [overview-panel])
(defmethod routing/header :routing.data/overview []
  [structure/header
   :label "Data Overview"])
