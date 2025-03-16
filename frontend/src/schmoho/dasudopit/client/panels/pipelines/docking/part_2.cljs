(ns schmoho.dasudopit.client.panels.pipelines.docking.part-2
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.common.forms :as forms]
   [schmoho.dasudopit.client.common.http :as http]
   [schmoho.dasudopit.client.common.views.widgets :as widgets]
   [schmoho.dasudopit.client.css.forms :as css]
   [schmoho.dasudopit.client.utils :as utils]))

;; === Subs ===

(rf/reg-sub
 :forms.docking.part-2/taxon-model-resolved
 :<- [:forms.docking.part-1/taxon-model]
 :<- [:data/taxons-map]
 (fn [[model taxons]]
   (map #(get taxons %) model)))

(rf/reg-sub
 :forms.docking.part-2/selected-proteins-model
 :<- [:forms/docking]
 (fn [form]
   (:selected-proteins-model form)))

(rf/reg-sub
 :forms.docking.part-2/selected-proteins-ids
 :<- [:forms.docking.part-2/selected-proteins-model]
 (fn [model]
   (->> model vals (map :id))))

(rf/reg-sub
 :forms.docking.part-2/selected-protein-for-taxon
 :<- [:forms.docking.part-2/selected-proteins-model]
 (fn
   [model [_ id]]
   (get model id)))

(rf/reg-sub
 :forms.docking.part-2/selected-protein-for-taxon-resolved
 :<- [:forms.docking.part-2/selected-proteins-model]
 :<- [:data/proteomes]
 (fn
   [[model proteomes] [_ taxon-id]]
   (let [selected-protein (:id (get model taxon-id))
         proteome         (get proteomes taxon-id)
         proteome (zipmap
                   (map :primaryAccession proteome)
                   proteome)]
     (get proteome selected-protein))))

(rf/reg-sub
 :forms.docking.part-2/valid?
 :<- [:forms.docking.part-1/taxon-model]
 :<- [:forms.docking.part-2/selected-proteins-ids]
 :<- [:data/structures]
 (fn [[taxa protein-ids structures]]
   (and (every? some? (map structures protein-ids))
        (= (count taxa)
           (count protein-ids))
        (pos? (count taxa)))))

;; === Views ===

(defn location-str
  [feature]
  (let [[start end] (utils/protein-feature->location feature)]
    (if (= start end)
      start
      (str start " - " end))))

(defn feature->hiccup
  [feature]
  [v
   :children
   [[h
     :gap "5px"
     :children
     [[:b (:type feature)]
      "at"
      [:span (location-str feature)]]]
    [:span (:description feature)]]])

(defn protein->has-afdb-hiccup
  [protein]
  (when (->> (:uniProtKBCrossReferences protein)
             (filter #(= "AlphaFoldDB" (:database %)))
             seq)
    [h :gap "5px" :children [[:span "AlphaFold"] [:i {:class "zmdi zmdi-check"}]]]))

(defn get-structures-button
  []
  (let [selected-proteins (rf/subscribe [:forms.docking.part-2/selected-proteins-ids])
        selected-taxa     (rf/subscribe [:forms.docking.part-1/taxon-model])
        hover?            (r/atom false)]
    (fn []
      (when (and (every? some? @selected-proteins)
                 (seq @selected-proteins)
                 (= (count @selected-taxa) (count @selected-proteins)))
        [com/button
         :src       (at)
         :label    "GET STRUCTURES"
         :class    (css/rectangle-button)
         :style    {:background-color "#0072bb"}
         :on-click (fn []
                     (doseq [p @selected-proteins]
                       (rf/dispatch [::http/http-get [:data :raw :structure p]])))
         :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
         :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                    :on-mouse-out  (com/handler-fn (reset! hover? false))}]))))

(defn protein-info
  [protein]
  [v
   :gap "5px"
   :children
   (into
    [(protein->has-afdb-hiccup protein)]
    (->> (:features protein)
         (filter #(#{"Active site" "Binding site"} (:type %)))
         (map feature->hiccup)))])

(defn hint
  []
  (when-not @(rf/subscribe [:forms.docking.part-2/valid?])
    [:span "Please choose a protein for each taxon and press the button to get the structures."]))

(defn part-2
  []
  (let [taxons @(rf/subscribe [:forms.docking.part-2/taxon-model-resolved])
        proteome-searchers
        (->> taxons
             (mapv
              (fn [taxon]
                ^{:key (:id taxon)}
                [v
                 :children
                 [[:h6 (:scientificName taxon)]
                  [widgets/protein-search
                   :proteome  @(rf/subscribe [:data/proteome (:id taxon)])
                   :model     (rf/subscribe [:forms.docking.part-2/selected-protein-for-taxon (:id taxon)])
                   :on-change #(rf/dispatch [::forms/set-form-data
                                             :docking
                                             :selected-proteins-model
                                             (:id taxon)
                                             %])]
                  (let [protein @(rf/subscribe [:forms.docking.part-2/selected-protein-for-taxon-resolved (:id taxon)])]
                    [protein-info protein])]])))]
    [v
     :children
     [[hint]
      [h
       :min-height "300px"
       :gap "30px"
       :children
       (into [] proteome-searchers)]
      [get-structures-button]]]))
