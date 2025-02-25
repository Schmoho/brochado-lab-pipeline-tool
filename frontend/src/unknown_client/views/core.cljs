(ns unknown-client.views.core
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.about :as about]
   [unknown-client.views.home :as home]
   [unknown-client.views.common :as common]))

(defmethod routes/panels :home [] [home/home-panel])
(defmethod routes/panels :about [] [about/about-panel])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(routes/panels @active-panel)]]))

#_(.reload js/location)
