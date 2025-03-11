(ns unknown-client.views.css.forms
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

