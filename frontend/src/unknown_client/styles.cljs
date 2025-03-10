(ns unknown-client.styles
  (:require-macros
   [garden.def :refer [defcssfn]])
  (:require
   [garden.core :refer [css]]
   [spade.core   :refer [defglobal defclass]]
   [garden.units :refer [deg px]]
   [garden.color :refer [rgba]]))

(defcssfn linear-gradient
  ([c1 p1 c2 p2]
   [[c1 p1] [c2 p2]])
  ([dir c1 p1 c2 p2]
   [dir [c1 p1] [c2 p2]]))

(defglobal defaults
  [:body
   {:color               :black
    #_#_:background-color    :#ddd
    #_#_:background-image    [(linear-gradient :white (px 2) :transparent (px 2))
                              (linear-gradient (deg 90) :white (px 2) :transparent (px 2))
                              (linear-gradient (rgba 255 255 255 0.3) (px 1) :transparent (px 1))
                              (linear-gradient (deg 90) (rgba 255 255 255 0.3) (px 1) :transparent (px 1))]
    :background-size     [[(px 100) (px 100)] [(px 100) (px 100)] [(px 20) (px 20)] [(px 20) (px 20)]]
    :background-position [[(px -2) (px -2)] [(px -2) (px -2)] [(px -1) (px -1)] [(px -1) (px -1)]]}])

(defclass header
  []
  {:color            :white
   :align-items      "center"
   :font-weight      "800"
   :text-align       "center"
   :background-color :#4db6ac})

(defclass navbar
  []
  {#_#_:border  "solid black 1px"})

(defclass content
  []
  {:color :green
   :border  "solid black 1px"})

;; (defclass level1
;;   []
;;   {:color :green
;;    :border-color :black})

(defclass footer
  []
  {:color    :green
   :position "absolute"
   :bottom   "0"
   :width    "100%"
   :border   "solid black 1px"})

(defclass footer-link
  []
  {:padding "0px 5px"})

(defclass inner-page-link
  []
  {:color         "white"
   :width         "100%"
   :font-size     "24"
   :font-weight   "400"
   :border        "none"
   :border-radius "0px"
   :padding       "10px 10px"
   :margin        "5px 0px"})

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

(defclass form-section
  []
  {:font-size   "22"
   :font-weight "300"
   
   :padding     "15px 15px"})

(defclass section-header
  []
  {:font-size   "150%"
   :font-weight "300"})


(defclass card-hover
  []
  {:background-color "#f0f8ff"
   :box-shadow       "0 4px 8px rgba(0, 0, 0, 0.2)"
   :cursor           "pointer"
   :transition       "background-color 0.3s ease, box-shadow 0.3s ease"})
