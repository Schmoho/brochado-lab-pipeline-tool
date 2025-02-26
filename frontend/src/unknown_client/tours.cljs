(ns unknown-client.tours
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com]
   [reagent.core :as r]
   [unknown-client.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def active-tour (r/atom nil))

(def tour-lookup
  {:taxonomic-comparison [:help-buttons]})

(re-frame/reg-fx
 :start-tour
 (fn [tour]
   (reset! active-tour tour)
   (re-com/start-tour tour)))
