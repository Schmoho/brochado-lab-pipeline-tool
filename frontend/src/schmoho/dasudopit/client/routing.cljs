(ns schmoho.dasudopit.client.routing
  (:require
   [bidi.bidi :as bidi]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [pushy.core :as pushy]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.panels.results.msa.events :as msa-results-events]
   [schmoho.dasudopit.client.utils :as utils]))

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])
(defmulti header identity)
(defmethod header :default [] [:div "No header found for this route."])

(def routes
  (atom
   ["/" {""           :routing/home
         "data/"      {"overview"       :routing.data/overview
                       "taxon"          :routing.data/taxon
                       "taxon/"         {#{[:taxon/id ""] [:taxon/id "/"]} :routing.data/taxon-entry}
                       "ligand"         :routing.data/ligand
                       "ligand/"        {#{[:ligand/id ""] [:ligand/id "/"]} :routing.data/ligand-entry}
                       "protein/"       {#{[:protein/id ""] [:protein/id "/"]} :routing.data/protein-entry}
                       "upload"         :routing.data/upload
                       "volcano/"        {#{[:volcano/id ""] [:volcano/id "/"]} :routing.data/volcano}}
         "pipelines/" {"msa"     :routing.pipelines/msa
                       "docking" :routing.pipelines/docking}
         "volcano-viewer" {"" :routing/volcano-viewer
                           "/" :routing/volcano-viewer}
         "results/"   {"msa-results"     :routing.results/msa
                       "docking-results" :routing.results/docking}}]))

(defn parse
  [url]
  (bidi/match-route @routes url))

(defn url-for
  [& args]
  (apply bidi/path-for (into [@routes] args)))

(defn dispatch
  [route]
  (rf/dispatch [::set-active-route route]))

(defonce history
  (pushy/pushy dispatch parse))

(defn navigate!
  [handler]
  (pushy/set-token! history
                    (if (coll? handler)
                      (apply url-for handler)
                      (url-for handler))))

(defn start!
  []
  (pushy/start! history))


;; === Events ===

(rf/reg-event-fx
 ::navigate
 (fn-traced [_ [_ handler]]
            {:navigate handler}))

(rf/reg-event-fx
 ::set-active-route
 (fn-traced [{:keys [db]} [_ route]]
            {:db             (assoc db
                                    :active-route route
                                    :active-panel (:handler route))
             :get-route-data route}))


(rf/reg-fx
 :navigate
 (fn [handler]
   (navigate! handler)))

(rf/reg-fx
 :get-route-data
 (fn [{:keys [handler route-params]}]
   (case handler
     :routing.results/msa-results
     (rf/dispatch [::msa-results-events/get-msa-results])
     :routing.data/overview
     (do
       (utils/get-data [:data :input :volcano])
       (utils/get-data [:data :raw :ligand])
       (utils/get-data [:data :raw :taxon]))
     :routing.data/taxon-entry
     (utils/get-data [:data :raw :taxon (:taxon/id route-params)])
     :routing.data/taxon
     (utils/get-data [:data :raw :taxon])
     :routing.data/ligand-entry
     (utils/get-data [:data :raw :ligand (:ligand/id route-params)])
     :routing.data/ligand
     (utils/get-data [:data :raw :ligand])
     :routing.data/upload
     (utils/get-data [:data :raw :taxon])
     :routing/volcano-viewer
     (utils/get-data [:data :input :volcano])
     :routing.pipelines/docking
     (do
       (utils/get-data [:data :raw :ligand])
       (utils/get-data [:data :raw :taxon]))
     nil)))


;; === Subs ===

(rf/reg-sub
 ::active-route
 (fn [db _]
   (:active-route db)))

(rf/reg-sub
 ::active-panel
 :<- [::active-route]
 (fn [active-route]
   (:handler active-route)))

(rf/reg-sub
 ::active-route-params
 :<- [::active-route]
 (fn [active-route]
   (:route-params active-route)))
