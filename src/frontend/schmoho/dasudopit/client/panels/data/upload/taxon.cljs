(ns schmoho.dasudopit.client.panels.data.upload.taxon
  (:require
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.structure :as structure :refer [minicard]]
   [schmoho.components.uniprot :as uniprot]))

;; === Subs ===

(rf/reg-sub
 :provision.taxon/input-model
 :<- [:forms/by-path :provision/taxon]
 (fn [form]
   (:input form)))

(rf/reg-sub
 :provision.taxon/search-result
 :<- [:data/taxons-map]
 :<- [:provision.taxon/input-model]
 (fn [[taxons input]]
   (-> taxons (get input) :search)))

(rf/reg-sub
 :provision.taxon/search-running?
 :<- [::http/queries]
 :<- [:provision.taxon/input-model]
 (fn [[queries input]]
   (= (-> queries :get (get [:data :taxon input :search]))
      :running)))

(rf/reg-sub
 :provision.taxon/post-query-state
 :<- [:provision.taxon/input-model]
 :<- [::http/queries]
 (fn [[id queries]]
   (-> queries :post (get [:data :taxon id]))))

;; === Views ===

(defn- taxon-id-input
  []
  (let [input-model      (rf/subscribe [:provision.taxon/input-model])]
    [v
     :children
     [[components.forms/info-label
       "Uniprot/NCBI Taxonomy ID"
       [:<>
        [:p.info-heading "Organism ID"]
        [:p "You need to put in a Uniprot or NCBI Taxonomy ID. Note they are the same."]
        [com/hyperlink-href :src (at)
         :label  "Link to docs."
         :href   ""
         :target "_blank"]]]
      [com/input-text
       :model input-model
       :on-change #(rf/dispatch [::forms/set-form-data :provision/taxon :input %])]]]))

(defn- taxon-minicard
  [taxon]
  [minicard
   "Taxon"
   [v
    :children
    [[:span "ID: "
      [:a {:href (str "https://www.uniprot.org/taxonomy/" (:id taxon))
          :target "_blank"}
       (:id taxon)]]
     [:div
      {:style {:width "300px"}}
      [:p "Name: " (:scientificName taxon)]]
     [:p "Taxonomic rank: " (:rank taxon)]]]])

(defn- proteome-minicard
  [proteome]
  [minicard
   "Proteome"
   [v
    :children
    [[:span "ID: "
      [:a {:href (str "https://www.uniprot.org/proteome/" (:id proteome))
          :target "_blank"}
       (:id proteome)]]
     (when-let [redundant (:instead-of-redundant proteome)]
       [:span "(instead of redundant proteome "
        [:a {:href (str "https://www.uniprot.org/proteome/" redundant)
          :target "_blank"}
         redundant]
        ")"])
     [:br]
     [:p "Type: " (:proteomeType proteome)]
     [:p "Protein count: " (:proteinCount proteome)]]]])

(defn- taxon-previewer
  []
  (let [search-result @(rf/subscribe [:provision.taxon/search-result])
        input-model   (rf/subscribe [:provision.taxon/input-model])
        {:keys [taxon proteome]} search-result]
    [v
     :children
     [[h
       :children
       [[v
         :children
         [[taxon-minicard taxon]
          [proteome-minicard proteome]]]
        [com/gap :size "1"]
        [uniprot/lineage-widget (:lineage taxon)]]]
      ]]))

(defn provision-taxon-form
  []
  (let  [input-model      (rf/subscribe [:provision.taxon/input-model])
         search-results   (rf/subscribe [:provision.taxon/search-result])
         search-running?  (rf/subscribe [:provision.taxon/search-running?])
         post-query-state (rf/subscribe [:provision.taxon/post-query-state])]
    [v
     :children
     [[taxon-id-input]
      [h
       :children
       [[com/gap :size "1"]
        (when-not @search-results
          [structure/flex-horizontal-center
           [components.forms/action-button
            :label "Search"
            :on-click #(rf/dispatch [::http/http-get
                                     [:data :taxon @input-model :search]])]])]]
      (when (= :done @post-query-state)
        [:span "Successfully added taxon " @input-model ". Please note it can take half a minute until the proteome is available."])
      (when @search-running?
        [structure/flex-horizontal-center
         [com/throbber]])
      (when (and (not= :done @post-query-state)
                 (not-empty @search-results))
        [:<>
         [taxon-previewer]
         [structure/flex-horizontal-center
          [components.forms/action-button
           :label "Save"
           :on-click #(rf/dispatch [::http/http-post [:data :taxon @input-model]])]]])]]))
