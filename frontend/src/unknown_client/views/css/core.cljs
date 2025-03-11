(ns unknown-client.views.css.core
  (:require-macros
   [garden.def :refer [defcssfn]])
  (:require
   [spade.core   :refer [defglobal defclass]]
   [garden.units :refer [px]]))

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
