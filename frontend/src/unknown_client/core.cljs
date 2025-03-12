(ns unknown-client.core
  (:require
   [reagent.core :as r]
   ["react-dom/client" :refer [createRoot]]
   [re-frame.core :as re-frame]
   [unknown-client.subs.data]
   [unknown-client.subs.forms.core]
   [unknown-client.subs.forms.volcano-viewer]
   [unknown-client.subs.forms.docking]
   [unknown-client.subs.forms.msa]
   [unknown-client.subs.routing]
   [unknown-client.events.db :as db-events]
   [unknown-client.events.routing]
   [unknown-client.routing :as routing]
   [unknown-client.views.core :as views]
   [unknown-client.config :as config]))

(defn dev-setup
  []
  (when config/debug?
    (println "dev mode")))

(defonce root
  (createRoot (.getElementById js/document "app")))

(defn ^:dev/after-load mount-root
  []
  (.render root (r/as-element
                 [views/main-panel])))

(defn init
  []
  (routing/start!)
  (re-frame/dispatch-sync [::db-events/initialize-db])
  (dev-setup)
  (mount-root))

