(ns schmoho.dasudopit.client.utils.re-frame
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [schmoho.dasudopit.client.common.http :as http]))

(defn get-data
  [path]
  (when-not (get (@rf.db/app-db :queries) path)
    (rf/dispatch [::http/http-get path])))

