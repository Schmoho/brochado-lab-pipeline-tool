(ns unknown-client.views.pipelines.docking
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.routing :as routing]
   [unknown-client.views.css.forms :as css]
   [unknown-client.events.forms :as form-events]
   [unknown-client.utils :refer [cool-select-keys]]))

(defn docking-header
  []
  [re-com/title
   :src   (at)
   :label "Comparative Docking Pipeline"
   :level :level1
   ])


(defn protein-search-suggestions
  [proteome]
  (fn [s]
    (let [fragment (re-pattern (str "(?i)" (or s "")))]
      (->> proteome
           (keep (fn [protein]
                   (let [gene-name    (or (-> protein :genes first :geneName :value)
                                          (-> protein :genes first :orderedLocusNames :value)
                                          "-")
                         protein-name (or (-> protein :proteinDescription :recommendedName :fullName :value)
                                          (-> protein :proteinDescription :submissionNames first :fullName :value)
                                          "-")
                         protein-id   (or (-> protein :primaryAccession)
                                          "-")]
                     (when (or (re-find fragment gene-name)
                               (re-find fragment protein-name)
                               (re-find fragment protein-id))
                       {:id protein-id :protein-name protein-name :gene-name gene-name}))))
           (take 16)
           (into [])))))

(defn protein-searcher
  [taxon-id]
  (let [proteome @(re-frame/subscribe [:data/proteome taxon-id])]
    [re-com/typeahead
     :src (at)
     :data-source (protein-search-suggestions proteome)
     :suggestion-to-string #(:protein-name %)
     :render-suggestion
     (fn [{:keys [protein-name gene-name id]}]
       [re-com/hyperlink-href
        :label (str id " - " gene-name " - " protein-name)
        :href (str "/protein/" id)])
     :on-change
     #(re-frame/dispatch
       [::form-events/set-form-data
        :docking
        :selected-proteins-model
        %])]))

(defn get-structures-button
  []
  (let [hover? (r/atom false)]
    (fn []
      [re-com/button
       :src       (at)
       :label    "GET STRUCTURES"
       :class    (css/rectangle-button)
       :style    {:background-color "#0072bb"}
       :on-click (fn [])
       :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
       :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                  :on-mouse-out  (re-com/handler-fn (reset! hover? false))}])))

(defn docking-panel
  []
  (let [form        (re-frame/subscribe [:forms/docking])
        ligands     (re-frame/subscribe [:data/ligands])
        taxons      (re-frame/subscribe [:data/taxons])
        taxon-model (re-frame/subscribe [:docking/taxon-model])
        internal-taxon-model (r/atom #{})
        internal-ligand-model (r/atom #{})]
    (fn []
      (prn @form)
      (let [ligand-model            (-> @form :ligand-model)
            taxon-choices           (->> @taxons
                                         (mapv #(cool-select-keys
                                                 %
                                                 [[:label :scientificName]
                                                  [:id [:taxonId str]]])))
            ligand-choices          (->> @ligands
                                         (mapv #(cool-select-keys
                                                 %
                                                 [[:label :name]
                                                  [:id [:json :id :id :cid]]])))
            proteome-searchers
            (->> @taxon-model
                 (mapv
                  (comp
                   (fn [[taxon-id taxon-name]]
                     [:div
                      [:p taxon-name]
                      [protein-searcher taxon-id]])
                   (fn [taxon-id]
                     [taxon-id (:scientificName @(re-frame/subscribe [:data/taxon taxon-id]))]))))
            selected-proteins-model (-> @form :selected-proteins-model)]
        [v
         :children
         [[h
           :src      (at)
           :children
           [(if (empty? taxon-choices)
                [v
                 :justify :center
                 :children
                 [[re-com/throbber
                   :size :large
                   :style {:width "410px"}]]]
                [re-com/multi-select :src (at)
                 :choices       taxon-choices
                 :model         internal-taxon-model
                 :on-change     #(do
                                   (reset! internal-taxon-model %)
                                   (re-frame/dispatch
                                    [::form-events/set-form-data
                                     :docking
                                     :taxon-model
                                     %]))
                 :width         "450px"
                 :left-label    "Available taxons"
                 :right-label   "Selected taxons"
                 :placeholder   "Select some taxons."
                 :required? true
                 :filter-box? false])
            [re-com/gap :size "50px"]
            [re-com/multi-select :src (at)
             :choices       ligand-choices
             :model         ligand-model
             :on-change     #(do
                               (reset! internal-taxon-model %)
                               (re-frame/dispatch
                                [::form-events/set-form-data
                                 :docking
                                 :ligand-model
                                 %]))
             :width         "450px"
             :left-label    "Available ligands"
             :right-label   "Selected ligands"
             :placeholder   "Select at least one ligand."
             :required? true
             :filter-box? false]]]
          [v
           :children
           (into [] proteome-searchers)]
          (when (some? selected-proteins-model)
            [v
             :children
             [[get-structures-button]
              [:div
               {:id "bla"
                :class "viewer_3Dmoljs"
                :style {:height "400px"
                        :width "400px"
                        :position  "relative"}
                ;; :width "600px"
                ;; :height "600px"
                :data-pdb "2POR"
                :data-backgroundcolor "0xffffff"
                :data-style "stick"
                :data-ui true}]]])]]))))



(defmethod routing/panels :docking [] [docking-panel])
(defmethod routing/header :docking [] [docking-header])
