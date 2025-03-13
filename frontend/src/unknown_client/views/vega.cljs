(ns unknown-client.views.vega
  (:require
   [unknown-client.utils :as utils]
   [reagent.core :as r]
   ["vega-embed" :default vegaEmbed]))

(defn vega-chart
  [& {:keys [width height id spec on-change]}]
  (r/create-class
   {:component-did-mount
    (fn [_]
      (-> (vegaEmbed (str "#" id) (clj->js spec))
          (.then #(when on-change
                    (on-change (.-view %))))))
    :reagent-render
    (fn []
      [:div {:id id
             :style {:width width
                     :height height}}])}))


;; (defn- value->css [v]
;;   (cond
;;     (number? v)  (str v "px")
;;     (keyword? v) (name v)
;;     (vector? v)  (str/join " " (map value->css v))
;;     (list? v)    (str (first v)
;;                       "("
;;                       (str/join ", " (map value->css (rest v)))
;;                       ")")
;;     :else        v))

;; (def exclude? #{:opacity :z-index})

;; (defn style->css [style]
;;   (reduce-kv
;;    (fn [css k v]
;;      (str
;;       css
;;       (when (and k v)
;;         (str (value->css k) ":"
;;              (if (exclude? k)
;;                v
;;                (value->css v)) ";")))) "" style))


;; (defn map->css [m]
;;   (reduce-kv
;;    (fn [css k v]
;;      (str css
;;           (str/join " " (map name k))
;;           "{" (style->css v) "}\n"))
;;    ""
;;    m))

;; (defn styles
;;   "CSS styles applied to the vega embed elements. Allow filling most of the container."
;;   []
;;   [:style
;;    (map->css
;;     {[:.vega-embed :.chart-wrapper]
;;      {:width "fit-content"
;;       :height "fit-content"}
;;      [:.vega-embed]
;;      {:width "100%"}
;;      [:.vega-embed :summary]
;;      {:opacity 1
;;       :cursor :default
;;       :position :absolute
;;       :right 3
;;       :top 3
;;       :z-index 0
;;       :transform "scale(0.6)"}})])

;; (defn- default-config
;;   []
;;   {:padding    {:bottom 3
;;                 :top    (* 3 3)
;;                 :left   (* 4 3)
;;                 :right  (* 4 3)}
;;    :autosize {:type "fit-x" :resize true :contains "padding"}
;;    :config {:legend {:labelColor "black"
;;                      :titleColor "black"}
;;             :view {:stroke "transparent"}
;;             :axis {:domainColor "black"
;;                    :domainWidth "3"
;;                    :tickColor "black"
;;                    :gridColor "black"
;;                    :gridDash    [10 2]
;;                    :titleColor "black"
;;                    :labelColor "black"}}})

;; (defn- deep-merge
;;   "Recursively merges maps.
;;    http://dnaeon.github.io/recursively-merging-maps-in-clojure/"
;;   [& maps]
;;   (letfn [(m [& xs]
;;             (if (some #(and (map? %) (not (record? %))) xs)
;;               (apply merge-with m xs)
;;               (last xs)))]
;;     (reduce m maps)))

;; (defmacro use-effect [& body]
;;   `(react/useEffect
;;     (fn []
;;       (let [result# (do ~@body)]
;;         (if (fn? result#) result# js/undefined)))))

;; (defn- use-resize []
;;   (let [ref              (react/useRef nil)
;;         [rect set-rect!] (react/useState #js {:height 200 :width 200})]
;;     (use-effect
;;      #js [(.-current ref)]
;;      (when-let [el (.-current ref)]
;;        (let [resize-observer
;;              (js/ResizeObserver.
;;               (fn []
;;                 (set-rect! (.getBoundingClientRect el))))]
;;          (.observe resize-observer el)
;;          (fn []
;;            (.disconnect resize-observer)))))
;;     [ref rect]))

;; (def default-config
;;   (let [text "black"
;;         border "black"
;;         padding 3]
;;     {:padding {:bottom padding
;;                :top (* 3 padding)
;;                :left (* 4 padding)
;;                :right (* 4 padding)}
;;      :autosize
;;      {:type "fit-x" :resize true :contains "padding"}
;;      :config
;;      {:legend
;;       {:labelColor text
;;        :titleColor text}
;;       :view
;;       {:stroke "transparent"}
;;       :axis
;;       {:domainColor border
;;        :domainWidth "3"
;;        :tickColor border
;;        :gridColor border
;;        :gridDash [10 2]
;;        :titleColor text
;;        :labelColor text}}}))

;; (defn vega-embed [opts value]
;;   (let [doc (deep-merge default-config value {:title ""})
;;         view                     (react/useRef nil)
;;         [init set-init!]         (react/useState false)
;;         [absolute absolute-rect] (use-resize)
;;         height                   (.-height absolute-rect)
;;         [relative relative-rect] (use-resize)
;;         width                    (.-width relative-rect)]
;;     (use-effect
;;      (when-let [el (.-current absolute)]
;;        (-> (vegaEmbed el
;;                       (clj->js (assoc doc :width width))
;;                       (clj->js opts))
;;            (.then (fn [value]
;;                     (set! (.-current view) (.-view value))
;;                     (set-init! true)))
;;            (.catch (fn [err] (js/console.error err)))))
;;      #(when-let [view (.-current view)]
;;         (.finalize view)
;;         (set! (.-current view) nil)))

;;     (use-effect
;;      #js [init (.-current view) width]
;;      (when-let [view (.-current view)]
;;        (let [width (- width 8)]
;;          (.width view width)
;;          (.run view))))

;;     [:div
;;      (when-let [title (:title value)]
;;        [:h1 title])
;;      (when-let [description (:description value)]
;;        [:p description])
;;      [:div
;;       {:ref relative
;;        :style
;;        {:min-width     400
;;         :height        height
;;         :position      :relative}}
;;       [:div#viz
;;        {:ref absolute
;;         :style
;;         {:position   :absolute
;;          :top        0
;;          :right      0
;;          :left       0
;;          :box-sizing :border-box
;;          :padding    6
;;          :overflow   :hidden}}]]]))

;; (defn vega-viewer [value]
;;   [vega-embed {:mode "vega" :renderer :svg} value])

;; (defn vega-lite-viewer [value]
;;   [vega/vega-embed
;;    {:mode "vega-lite" :renderer :svg}
;;    value])
