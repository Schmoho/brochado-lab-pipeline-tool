(ns pipeline.visualization
  (:require
   [oz.core :as oz]))

(oz/start-server!)

(defn play-data
  [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

(def line-plot
  {:data     {:values (play-data "monkey" "slipper" "broom")}
   :encoding {:x     {:field "time" :type "quantitative"}
              :y     {:field "quantity" :type "quantitative"}
              :color {:field "item" :type "nominal"}}
   :mark     "line"})

;; Render the plot
(oz/view! line-plot)


(def stacked-bar
  {:data     {:values (play-data "munchkin" "witch" "dog" "lion" "tiger" "bear")}
   :mark     "bar"
   :encoding {:x     {:field "time"
                      :type  "ordinal"}
              :y     {:aggregate "sum"
                      :field     "quantity"
                      :type      "quantitative"}
              :color {:field "item"
                      :type  "nominal"}}})

(oz/view! stacked-bar)


(def contour-plot (oz/load "examples/contour-lines.vega.json"))
(oz/view! contour-plot :mode :vega)

(def viz
  [:div
    [:h1 "Look ye and behold"]
    [:p "A couple of small charts"]
    [:div {:style {:display "flex" :flex-direction "row"}}
      [:vega-lite line-plot]
      [:vega-lite stacked-bar]]
    [:p "A wider, more expansive chart"]
    [:vega contour-plot]
    [:h2 "If ever, oh ever a viz there was, the vizard of oz is one because, because, because..."]
    [:p "Because of the wonderful things it does"]])

(oz/view! viz)

(oz/export! viz "test.html")
