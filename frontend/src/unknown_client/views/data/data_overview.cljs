(ns unknown-client.views.data.data-overview
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v h-box h}]
   [re-frame.core :as re-frame]
   [unknown-client.routing :as routing]
   [unknown-client.subs.routing :as routing-subs]
   [unknown-client.views.common.widgets :as widgets]))


(defn overview-header []
  [com/title
   :src   (at)
   :label "Data Overview"
   :level :level1])

(defn table
  [data & {:keys [columns]}]
  (if (nil? @data)
    [com/throbber :size :regular]
    [v
     :width "1550px"
     :max-width "1550px"
     :children
     [[h
       :children
       [[com/simple-v-table
         :src                       (at)
         :model data
         :max-width "1000px"
         :columns
         (mapv (fn [defaults input]
                 (merge defaults input))
               (map (fn [col]
                      (assoc
                       {:width 300
                        :align "center"
                        :vertical-align "middle"}
                       :row-label-fn #((:id col) %)
                       :header-label (name (:id col))))
                    columns)
               columns)
         :row-height                35]]]]]))

(defn overview-panel []
  (let [volcanos (re-frame/subscribe [:data/volcanos])
        taxons (re-frame/subscribe [:data/taxons])
        ligands (re-frame/subscribe [:data/ligands])]
    (fn []
      [v
       :children
       [[table volcanos
         :columns
         [{:id :name
           :row-label-fn
           (fn [row]
             [:a {:href (str "volcano/" (-> row :meta :id))}
              (or (-> row :meta :name not-empty)
                  (-> row :meta :id))])
           :header-label "Dataset Name"}
          {:id :scientificName
           :header-label "Name"}]]
        [table taxons
         :columns
         [{:id :id
           :row-label-fn (fn [row]
                           [:a {:href (str "taxon/" (:id row))}
                            (:id row)])
           :header-label "Taxon ID"}
          {:id :scientificName
           :header-label "Name"}]]
        [table ligands
         :columns
         [{:id :id
           :row-label-fn (fn [row]
                           [:a {:href (str "ligand/" (:id row))}
                            (:id row)])
           :header-label "Ligand ID"}
          {:id :name
           :header-label "Name"}]]]])))

;; (defn colfn
;;   [data & {:keys [columns]}]
;;   (map (fn [defaults input]
;;                 (merge defaults input))
;;               (map #(assoc
;;                      {:width 300
;;                       :align "center"
;;                       :vertical-align "middle"}
;;                      :row-label-fn (:id %)
;;                      :header-label (name (:id %)))
;;                    columns)
;;               columns))
;; (colfn @(re-frame/subscribe [:data/taxons])
;;        :columns [{:id :id}
;;                  {:id :scientificName}])

(defmethod routing/panels :routing.data/overview [] [overview-panel])
(defmethod routing/header :routing.data/overview [] [overview-header])
