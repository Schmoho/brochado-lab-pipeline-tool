(ns unknown-client.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [unknown-client.events :as events]))

(defmulti panels identity)
(defmethod panels :default []
  [:div "No panel found for this route."])

(defmulti header identity)
(defmethod header :default []
  [:div "No header found for this route."])

(def routes
  (atom
   ["/" {""        :home
         #_#_"taxon/" {#{[:taxons/id ""]
                     [:taxons/id "/"]} :taxons
                   [:taxons/id "/proteome/"]
                   {[:proteomes/id ""] :proteomes}}
         "about"   :about
         "taxonomic-comparison" :taxonomic-comparison
         "structural-comparison" :structural-comparison}]))


(defn parse
  [url]
  (bidi/match-route @routes url))

(defn url-for
  [& args]
  (apply bidi/path-for (into [@routes] args)))

(defn dispatch
  [route]
  (re-frame/dispatch [::events/set-active-route route]))

(defonce history
  (pushy/pushy dispatch parse))

(defn navigate!
  [handler]
  (prn handler)
  (pushy/set-token! history (if (coll? handler)
                              (apply url-for handler)
                              (url-for handler))))

(defn start!
  []
  (pushy/start! history))

(re-frame/reg-fx
 :navigate
 (fn [handler]
   (navigate! handler)))

