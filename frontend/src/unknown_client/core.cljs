(ns unknown-client.core
  (:require
   [re-com.core :as re-com :refer [simple-v-table]]
   [reagent.core :as r]
   ["react-dom/client" :refer [createRoot]]
   [goog.dom :as gdom]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.views.core :as views]
   [unknown-client.config :as config]
   [unknown-client.fasta :refer [f]]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defonce root (createRoot (.getElementById js/document "app")))


(defn ^:dev/after-load mount-root []
  (.render root (r/as-element
                 [views/main-panel])))

(defn init []
  (routes/start!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))

