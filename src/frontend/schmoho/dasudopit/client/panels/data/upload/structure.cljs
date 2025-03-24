(ns schmoho.dasudopit.client.panels.data.upload.structure
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.db :as db]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.pdb :as components.pdb]
   [schmoho.components.pdb :as pdb]
   [schmoho.components.uniprot :as uniprot]))

(def form-model
  {:taxon   [:upload/structure :taxon]
   :protein [:upload/structure :protein]
   :name    [:upload/structure :meta :name]
   :pdb     [:upload/structure :pdb]})

(def model (partial forms/model form-model))
(def setter (partial forms/setter form-model))

(defn- protein-name-input
  []
  [v
   :children
   [[components.forms/info-label "Required: Name" [:div ""]]
    [components.forms/input-text
     :on-change (setter :name)
     :placeholder "Insert a name for the structure"]]])

(defn- handle-choose-taxon
  [taxon]
  ((setter :taxon) taxon)
  (db/get-data [:data :taxon taxon :proteome]))

(defn- taxon-chooser
  []
  (let [taxon-choices @(rf/subscribe [:data/taxon-choices])]
    [v
     :children
     [[components.forms/info-label "Required: Taxon" [:<>]]
      (let [selection-model model]
        [com/single-dropdown
         :choices taxon-choices
         :model selection-model
         :on-change #(handle-choose-taxon %)
         :placeholder "For which taxon?"])]]))

(defn- pdb-file-upload
  []
  [v
   :children
   [[components.forms/info-label
     "Required: Structure File"
     [:<>]]
    [components.pdb/pdb-upload
     :on-load (setter :pdb)]]])

(defn- protein-chooser
  []
  (let [taxon-model   (model :taxon)
        protein-model (model :protein)]
    [uniprot/taxon-protein-search
     :taxon @taxon-model
     :protein-model protein-model
     :on-change (setter :protein)]))

(defn upload-structure-form
  []
  [h
   :children
   [[v
     :children
     [[protein-name-input]
      [taxon-chooser]
      [protein-chooser]]]
    [pdb-file-upload]
    (let [protein @(rf/subscribe [:data/protein (:id @(model :protein))])
          pdb     @(model :pdb)]
      (when (and protein pdb)
        [pdb/structural-features-viewer
         pdb
         protein]))]])
