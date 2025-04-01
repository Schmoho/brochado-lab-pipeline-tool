(ns schmoho.dasudopit.client.panels.data.upload.structure
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.panels.data.subs]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.pdb :as pdb]
   [schmoho.components.uniprot :as uniprot]
   [schmoho.components.structure :as structure]
   [schmoho.dasudopit.client.http :as http]))

(def form-model
  {:taxon             [:upload/structure :taxon]
   :protein           [:upload/structure :protein]
   :name              [:upload/structure :meta :name]
   :pdb               [:upload/structure :pdb]
   :file-upload-label [:upload/structure :file-upload-label]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(rf/reg-sub
 :upload.structure/form-valid?
 :<- [:forms/by-path :upload/structure]
 (fn [form]
   (and (some? (not-empty (-> form :meta :name)))
        (some? (not-empty (-> form :protein :id)))
        (some? (not-empty (-> form :taxon)))
        (some? (not-empty (-> form :pdb))))))

(rf/reg-sub
 :upload.structure/post-query-state
 :<- [::http/queries]
 :<- [:forms/by-path :upload/structure :protein]
 (fn [[queries protein]]
   (-> queries :post (get [:data :structure (:id protein) "input"]))))

(defn upload-structure-form
  []
  (let [taxon-model   (model :taxon)
        protein-model (model :protein)
        pdb           (model :pdb)
        taxon-choices @(rf/subscribe [:data/taxon-choices])
        protein       (rf/subscribe [:data/protein (:id @protein-model)])
        proteome      @(rf/subscribe [:data/proteome @taxon-model])
        form-valid?   @(rf/subscribe [:upload.structure/form-valid?])
        query-state   @(rf/subscribe [:upload.structure/post-query-state])]
    [v
     :children
     [[h
       :children
       [[v
         :children
         [[uniprot/taxon-chooser
           :choices taxon-choices
           :model (model :taxon)
           :on-change (fn handle-choose-taxon
                        [taxon]
                        ((setter :taxon) taxon)
                        (db/get-data [:data :taxon taxon :proteome]))]
          [uniprot/protein-search
           :proteome  proteome
           :model     protein-model
           :on-change (setter :protein)]]]
        [v
         :children
         [[components.forms/input-text
           :model (model :name)
           :on-change (setter :name)
           :placeholder "Insert a name for the structure"]
          [pdb/pdb-upload
           :label @(model :file-upload-label)
           :on-load #(do
                       ((setter :file-upload-label) %1)
                       ((setter :pdb) %2))]]]]]
      (cond
        (and (= query-state :done) form-valid?)
        [:p "Successfully added structure with name " @(model :name)]
        (= query-state :running)
        [com/throbber :size :large]
        :else
        [:<>
         [h
          :gap "10px"
          :children
          [(when (and #_@protein @pdb)
             [pdb/structural-features-viewer
              :structure-reload? true
              :pdb @pdb
              :uniprot @protein
              :style {:width      "240px"
                      :height     "100%"
                      :min-height "240px"}])
           (when @protein
             [uniprot/protein-structural-features-overview @protein])]]
         [structure/flex-horizontal-center
          [components.forms/action-button
           :disabled? (not form-valid?)
           :label "Save"
           :on-click #(rf/dispatch
                       [::http/http-post
                        [:data :structure (:id @protein) "input"]
                        {:params
                         {:structure @pdb
                          :meta      {:protein (:id @protein)
                                      :name    @(model :name)
                                      :taxon   @taxon-model
                                      :source  :input}}}])]]])]]))



#_(-> @re-frame.db/app-db :data :taxon (get "208963") :proteome :data (get "A0A0H2ZHP9"))
