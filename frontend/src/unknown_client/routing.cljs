(ns unknown-client.routing
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [unknown-client.events.routing :as routing-events]
   [unknown-client.events.http :as http-events]
   [unknown-client.utils :as utils]))

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



(re-frame/reg-fx
 :get-route-data
 (fn [route]
   (case route
     :routing.results/msa-results
     (re-frame/dispatch [::http-events/get-msa-results])
     :routing.data/overview
     (do
       (utils/get-data [:data :input :volcano])
       (utils/get-data [:data :raw :ligand])
       (utils/get-data [:data :raw :taxon]))
     :routing.data/taxon
     (utils/get-data [:data :raw :taxon])
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

