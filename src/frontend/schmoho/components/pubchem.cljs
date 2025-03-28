(ns schmoho.components.pubchem
  (:require
   [re-com.core :as com :refer [at] :rename {v-box v}]
   [re-frame.core :as rf]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.forms :as components.forms]))

(defn ligand-viewer
  [ligand]
  (let [{:keys [meta image]} ligand]
    [v
     :align :center
     :children
     [[com/title :label (:title meta)
       :level :level4]
      [com/gap :size "1"]
      [:img {:src   (str "data:image/png;base64," image)
             :style {:max-width  "250px"
                     :max-height "250px"
                     :border     "1px solid #ddd"}}]]]))
