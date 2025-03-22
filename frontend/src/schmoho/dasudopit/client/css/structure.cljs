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

(def carousel-control-base
  {:height "100px"
   :width "100px"
   :outline "black"
   :background-size "100%, 100%"
   :border-radius "50%"
   :background-image "none"})

(defclass my-carousel-control-prev-icon []
  carousel-control-base
  [:&:after {:content "\"<\""
             :font-size "55px"
             :color "#787878"}])

(defclass my-carousel-control-next-icon []
  carousel-control-base
  [:&:after {:content "\">\""
             :font-size "55px"
             :color "#787878"}])

