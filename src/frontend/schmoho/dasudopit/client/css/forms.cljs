(ns schmoho.dasudopit.client.css.forms
  (:require
   [spade.core :refer-macros [defclass]]))

(defclass form-section
  []
  {:font-size   "22"
   :font-weight "300" 
   :padding     "15px 15px"})

(defclass section-header
  []
  {:font-size   "150%"
   :font-weight "300"})

(defclass help-thingie
  []
  {:padding "0px 10px"})

(defclass plus-button
  []
  {:padding "10px 10px"})

(defclass checkbox-container
  []
  {:font-size        "22"
   :font-weight      "300"
   :border           "solid black 1px"
   :padding          "10px 20px"})

(defclass rectangle-button
  []
  {:color         "white"
   :width         "100%"
   :font-size     "24"
   :font-weight   "400"
   :border        "none"
   :border-radius "0px"
   :padding       "10px 10px"
   :margin        "5px 0px"})
