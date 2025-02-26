(ns unknown-client.views.structural-comparison
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


(defn structural-comparison-header
  []
  [re-com/title
   :src   (at)
   :label "Structure!"
   :level :level1
   :class (styles/header)])

(defn structural-comparison-panel []
  #_[v
     :src      (at)

     :children [[structural-comparison-title]
                [common/link-to-page "go to About Page" :about]]]
  [:p "Text"])





