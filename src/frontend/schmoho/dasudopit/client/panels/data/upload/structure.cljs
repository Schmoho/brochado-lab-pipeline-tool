(ns schmoho.dasudopit.client.panels.data.upload.structure
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.panels.data.subs]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.pdb :as pdb]
   [schmoho.components.uniprot :as uniprot]))

(def form-model
  {:taxon   [:upload/structure :taxon]
   :protein [:upload/structure :protein]
   :name    [:upload/structure :meta :name]
   :pdb     [:upload/structure :pdb]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(defn upload-structure-form
  []
  (let [taxon-model   (model :taxon)
        protein-model (model :protein)
        pdb           (model :pdb)
        taxon-choices @(rf/subscribe [:data/taxon-choices])
        protein       (rf/subscribe [:data/protein (:id @protein-model)])
        proteome      (-> @(rf/subscribe [:data/proteome @taxon-model]) :data)]
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
           :on-change (setter :protein)]
          ]]
        [v
         :children
         [[components.forms/input-text
           :on-change (setter :name)
           :placeholder "Insert a name for the structure"]
          [pdb/pdb-upload
           :on-load (setter :pdb)]]]]]
      [h
       :children
       [(when (and @protein @pdb)
          [pdb/structural-features-viewer @pdb @protein])
        (when @protein
          [uniprot/protein-structural-features-overview @protein])]]]]))
