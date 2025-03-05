(ns unknown-client.views.home
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.views.defs :as defs]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.common :refer [help-thingie checkbox]]))

(defn home-header []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Diese Seite braucht irgendeinen Titel!")
     :level :level1
     :class (styles/header)]))

(defn home-panel []
  [v
   :gap      "1em"
   :children [[h
               :height "100px"
               :children [[:h1 "Hallo Hallo!"]]]]])



