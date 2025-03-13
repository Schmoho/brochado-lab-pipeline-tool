(ns unknown-client.routing
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [re-frame.db :as re-frame.db]
   [unknown-client.events.routing :as routing-events]
   [unknown-client.events.http :as http-events]))

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
  (re-frame/dispatch [::routing-events/set-active-route route]))

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

(re-frame/reg-fx
 :navigate
 (fn [handler]
   (navigate! handler)))

(defn get-data
  [path]
  (when-not
      ((@re-frame.db/app-db :already-executed-queries) path)
    (re-frame/dispatch
     [::http-events/http-get path])))

(re-frame/reg-fx
 :get-route-data
 (fn [route]
   (case route
     :routing.results/msa-results
     (re-frame/dispatch [::http-events/get-msa-results])
     :routing.data/overview
     (do
       (get-data [:data :input :volcano])
       (get-data [:data :raw :ligand])
       (get-data [:data :raw :taxon]))
     :routing.data/taxon
     (get-data [:data :raw :taxon])
     :routing.data/ligand
     (get-data [:data :raw :ligand])
     :routing.data/upload
     (get-data [:data :raw :taxon])
     :routing/volcano-viewer
     (get-data [:data :input :volcano])
     :routing.pipelines/docking
     (do
       (get-data [:data :raw :ligand])
       (get-data [:data :raw :taxon]))
     nil)))

