(ns unknown-client.views.volcano
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [unknown-client.events :as events]
   [unknown-client.styles :as styles]
   [unknown-client.subs :as subs]))

(defn volcano-header []
  [re-com/title
   :src   (at)
   :label "This is the About Page."
   :level :level1])

(defn volcano-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[:p "Hi"]]])
