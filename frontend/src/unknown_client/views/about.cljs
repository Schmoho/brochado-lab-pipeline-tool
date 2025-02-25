(ns unknown-client.views.about
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.views.common :as common]))


(defn about-title []
  [re-com/title
   :src   (at)
   :label "This is the About Page."
   :level :level1])

(defn about-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[about-title]
              [common/link-to-page "go to Home Page" :home]]])
