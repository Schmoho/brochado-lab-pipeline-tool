(ns schmoho.dasudopit.client.css.structure
  (:require
   [spade.core :refer-macros [defclass]]))

(defclass clickable-card-hover
  []
  {:background-color "#f0f8ff"
   :box-shadow       "0 4px 8px rgba(0, 0, 0, 0.2)"
   :cursor           "pointer"
   :transition       "background-color 0.3s ease, box-shadow 0.3s ease"})

(defclass card-hover
  []
  {:background-color "#f0f8ff"
   :box-shadow       "0 4px 8px rgba(0, 0, 0, 0.2)"
   :transition       "background-color 0.3s ease, box-shadow 0.3s ease"})

