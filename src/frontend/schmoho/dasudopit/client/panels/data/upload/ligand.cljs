(ns schmoho.dasudopit.client.panels.data.upload.ligand
  (:require
   [re-com.core :as com :refer [at] :rename {v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.forms :as components.forms]))

;; === Subs ===

(rf/reg-sub
 :provision.ligand/search-term-model
 :<- [:forms/by-path :provision/ligand]
 (fn [form]
   (:search-term form)))

(rf/reg-sub
 :provision.ligand/search-result
 :<- [:data/ligands-map]
 :<- [:provision.ligand/search-term-model]
 (fn [[ligands input]]
   (-> ligands (get input) :search)))

(rf/reg-sub
 :provision.ligand/tab-model
 :<- [:forms/by-path :provision/ligand]
 :<- [:provision.ligand/search-result]
 (fn [[form search-result]]
   (or (:tab form)
       (-> search-result ffirst))))

(rf/reg-sub
 :provision.ligand/search-running?
 :<- [::http/queries]
 :<- [:provision.ligand/search-term-model]
 (fn [[queries input]]
   (= (get queries [:data :ligand input :search])
      :running)))

(rf/reg-sub
 :provision.ligand/post-query-state
 :<- [::http/queries]
 :<- [:provision.ligand/tab-model]
 (fn [[queries id]]
   (get queries [:data :ligand id])))

;; === Views ===

(defn- compound-search-input
  []
  (let [input-model (rf/subscribe [:provision.ligand/search-term-model])]
    [v
     :children
     [[components.forms/info-label
       "Pubchem Compound ID"
       [:<>
        [:p.info-heading "Compound name or Pubchem ID"]
        [:p (str "If you put in a name, please note that you might have to choose between multiple results. A tab bar will appear and let you choose the options."
                 "If you put in a Pubchem Compound ID, please note Pubchem distinguishes 'substances' and 'compounds'. We are going for compounds.")]
        [com/hyperlink-href :src (at)
         :label  "Link to docs."
         :href   ""
         :target "_blank"]]]
      [com/input-text
       :model input-model
       :on-change #(do (rf/dispatch [::forms/set-form-data :provision/ligand :search-term %])
                       (rf/dispatch [::forms/set-form-data :provision/ligand :tab nil]))]]]))

(defn- search-ligand-button
  []
  (let [input-model (rf/subscribe [:provision.ligand/search-term-model])]
    [components.forms/action-button
     :label "Search"
     :on-click #(rf/dispatch [::http/http-get
                              [:data :ligand @input-model :search]])]))

(defn- ligand-search-result
  [{:keys [meta png]}]
  [v
   :align :center
   :children
   [[com/title :label (:title meta)
     :level :level4]
    [com/gap :size "1"]
    [:img {:src   (str "data:image/png;base64," png)
           :style {:max-width  "250px"
                   :max-height "250px"
                   :border     "1px solid #ddd"}}]]])

(defn- ligand-previewer
  []
  (let [search-results (rf/subscribe [:provision.ligand/search-result])
        tab-model      (rf/subscribe [:provision.ligand/tab-model])]
    (fn []
      [v
       :children
       [[com/horizontal-bar-tabs
         :model tab-model
         :on-change #(rf/dispatch [::forms/set-form-data :provision/ligand :tab %])
         :tabs
         (->> @search-results
              (map (fn [[id data]]
                     {:id    id
                      :label (-> data :meta :title)}))
              vec)]
        [ligand-search-result
         (->> @search-results
              (filter
               (fn [[id _]]
                 (= @tab-model id)))
              first
              second)]
        [components.forms/action-button
         :label "Save"
         :on-click #(rf/dispatch [::http/http-post [:data :ligand @tab-model]])]]])))

(defn provision-ligand-form
  []
  (let [input-model      (rf/subscribe [:provision.ligand/search-term-model])
        search-results   (rf/subscribe [:provision.ligand/search-result])
        search-running?  (rf/subscribe [:provision.ligand/search-running?])
        post-query-state (rf/subscribe [:provision.ligand/post-query-state])]
    [v
     :children
     [[compound-search-input]
      [search-ligand-button]
      (when (= :done @post-query-state)
        [:span "Successfully added ligand " @input-model])
      (when @search-running?
        [com/throbber])
      (when (and (not= :done @post-query-state)
                 (not-empty @search-results))
        [ligand-previewer])]]))
