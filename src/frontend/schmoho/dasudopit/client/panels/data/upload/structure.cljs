(ns schmoho.dasudopit.client.panels.data.upload.structure
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.views.forms :as common.forms]
   [schmoho.dasudopit.client.common.views.protein :as protein]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.utils.re-frame :as utils]))

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
   [[common.forms/info-label "Required: Name" [:div ""]]
    [common.forms/input-text
     :on-change (setter :name)
     :placeholder "Insert a name for the structure"]]])

(defn- handle-choose-taxon
  [taxon]
  ((setter :taxon) taxon)
  (utils/get-data [:data :taxon taxon :proteome]))

(defn- taxon-chooser
  []
  (let [taxon-model (model :taxon)]
    [v
     :children
     [[common.forms/info-label "Required: Taxon" [:<>]]
      [widgets/taxon-chooser
       :model taxon-model
       :on-change #(handle-choose-taxon %)]]]))

(defn- pdb-file-upload
  []
  [v
   :children
   [[common.forms/info-label
     "Required: Structure File"
     [:<>]]
    [common.forms/pdb-upload
     :on-load (setter :pdb)]]])

(defn- protein-chooser
  []
  (let [taxon-model   (model :taxon)
        protein-model (model :protein)]
    [protein/taxon-protein-search
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
    [pdb-file-upload]]])
