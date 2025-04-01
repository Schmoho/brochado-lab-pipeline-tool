(ns schmoho.dasudopit.client.panels.data.upload.ligand
  (:require
   [re-com.core :as com :refer [at] :rename {v-box v h-box h}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.forms :as components.forms]
   [schmoho.components.pubchem :as pubchem]
   [schmoho.components.structure :as structure]))

(def form-model
  {:search-term [:provision/ligand :search-term]
   :tab         [:provision/ligand :tab]})

(def setter (partial forms/setter form-model))

;; === Subs ===

(rf/reg-sub
 :provision.ligand/search-term-model
 :<- [:forms/by-path :provision/ligand]
 (fn [form]
   (:search-term form)))

(rf/reg-sub
 :provision.ligand/search-result
 :<- [:forms/by-path :provision/ligand]
 :<- [:provision.ligand/search-term-model]
 (fn [[form input]]
   (-> form :search-result (get input))))

(rf/reg-sub
 :provision.ligand/tab-model
 :<- [:forms/by-path :provision/ligand]
 :<- [:provision.ligand/search-result]
 (fn [[form search-result]]
   (or (:tab form)
       (-> search-result first :meta :cid))))

(rf/reg-sub
 :provision.ligand/search-running?
 :<- [::http/queries]
 :<- [:provision.ligand/search-term-model]
 (fn [[queries input]]
   (= (-> queries :get (get [:data :ligand input :search]))
      :running)))

(rf/reg-sub
 :provision.ligand/save-running?
 :<- [::http/queries]
 :<- [:provision.ligand/tab-model]
 (fn [[queries input]]
   (= (-> queries :post (get [:data :ligand input]))
      :running)))

(rf/reg-sub
 :provision.ligand/post-save-state
 :<- [::http/queries]
 :<- [:provision.ligand/tab-model]
 (fn [[queries id]]
   (-> queries :post (get [:data :ligand id]))))

(rf/reg-sub
 :provision.ligand/post-search-state
 :<- [::http/queries]
 :<- [:provision.ligand/search-term-model]
 (fn [[queries input]]
   (-> queries :get (get [:data :ligand input :search]))))

(rf/reg-sub
 :provision.ligand/currently-active-preview
 :<- [:provision.ligand/search-result]
 :<- [:provision.ligand/tab-model]
 (fn [[search-result tab-model]]
   (->> search-result
        (filter
         (fn [data]
           (= tab-model (-> data :meta :cid))))
        first)))

;; === Views ===

(defn compound-search-info
  []
  [:<>
    [:p.info-heading "Compound name or Pubchem ID"]
    [:p (str "If you put in a name, please note that you might have to choose between multiple results. A tab bar will appear and let you choose the options."
             "If you put in a Pubchem Compound ID, please note Pubchem distinguishes 'substances' and 'compounds'. We are going for compounds.")]
    [com/hyperlink-href :src (at)
     :label  "Link to docs."
     :href   ""
     :target "_blank"]])


(defn- ligand-previewer
  []
  (let [search-results   (rf/subscribe [:provision.ligand/search-result])
        tab-model        (rf/subscribe [:provision.ligand/tab-model])
        currently-active (rf/subscribe [:provision.ligand/currently-active-preview])
        tabs             (->> @search-results
                              (map (fn [data]
                                     {:id    (-> data :meta :cid)
                                      :label (-> data :meta :title)}))
                              vec)]
    (fn []
      [v
       :children
       [[com/horizontal-bar-tabs
         :model tab-model
         :on-change (setter :tab)
         :tabs tabs]
        [pubchem/ligand-viewer @currently-active]]])))

(rf/reg-event-db
 ::ligand-search-success
 (fn [db [_ thing result]]
   (-> db
       (assoc-in [:forms :provision/ligand :search-result thing] result)
       (assoc-in [:queries :get [:data :ligand thing :search]] :done))))

(defn provision-ligand-form
  []
  (let [input-model       (rf/subscribe [:provision.ligand/search-term-model])
        tab-model         (rf/subscribe [:provision.ligand/tab-model])
        search-results    (rf/subscribe [:provision.ligand/search-result])
        search-running?   (rf/subscribe [:provision.ligand/search-running?])
        save-running?     (rf/subscribe [:provision.ligand/save-running?])
        post-save-state   (rf/subscribe [:provision.ligand/post-save-state])
        post-search-state (rf/subscribe [:provision.ligand/post-search-state])]
    [v
     :children
     [[components.forms/input-text
       :label       "Compound"
       :placeholder "Compound name or Pubchem ID"
       :model       input-model
       :on-change   #(do ((setter :search-term) %)
                         ((setter :tab) nil))
       :info-body [compound-search-info]]
      [structure/flex-horizontal-center
       (when (and (not @search-running?)
                  (not= :done @post-search-state))
          [components.forms/action-button
           :label    "Search"
           :on-click #(rf/dispatch [::http/http-get [:data :ligand @input-model :search]
                                    {:success-event [::ligand-search-success @input-model]}])])]
      (when @search-running?
        [structure/flex-horizontal-center
         [com/throbber :size :large]])
      (when (and (not= :done @post-save-state)
                 (not-empty @search-results)
                 (not @save-running?))
        [:<>
         [ligand-previewer]
         [structure/flex-horizontal-center
          [components.forms/action-button
           :label "Save"
           :on-click #(rf/dispatch [::http/http-post
                                    [:data :ligand @tab-model]])]]])
      (when @save-running?
        [structure/flex-horizontal-center
         [com/throbber :size :large]])
      (when (= :done @post-save-state)
        [:span "Successfully added ligand " @input-model])]]))
