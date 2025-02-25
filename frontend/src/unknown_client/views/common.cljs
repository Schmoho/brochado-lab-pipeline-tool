(ns unknown-client.views.common
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]))

(defn link-to-page
  [label path]
  [re-com/hyperlink
   :src      (at)
   :label    label
   :on-click #(re-frame/dispatch (if (coll? path)
                                   (into [::events/navigate]
                                         path)
                                   [::events/navigate path]))])
