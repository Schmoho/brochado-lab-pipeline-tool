(ns schmoho.dasudopit.client.common.views.vega
  (:require [reagent.core :as r]
            ["react" :as react]
            ;; ["react-vega" :refer [VegaLite]]
            ;; ["vega-tooltip" :refer [Handler]]
            ))

;; (def -vega-chart (r/adapt-react-class VegaLite))

;; (def default-config {:padding 5
;;                      :axis    {:labelFont       :inherit
;;                                :titleFont       :inherit
;;                                :titleFontSize   13
;;                                :titleFontWeight 500}
;;                      :legend  {:labelFont :inherit
;;                                :titleFont :inherit}
;;                      :title   {:font         :inherit
;;                                :subtitleFont :inherit}
;;                      :text    {:font :inherit}})

;; (defn chart [{:keys [init-width init-height init-size renderer]
;;               :or   {renderer :svg}}]
;;   (let [;; The width reacts to the viewport size changes,
;;         ;; the height stays the same.
;;         width    (r/atom (or init-width init-size 300))
;;         height   (or init-height init-size 300)
;;         observer (atom nil)
;;         ref      (react/createRef)]
;;     (r/create-class
;;      {:display-name
;;       "vega-chart"

;;       :reagent-render
;;       (fn [{:keys [spec data signal-handlers]}]
;;         [:div {:style {:width   "100%"
;;                        :display :inline-block}
;;                :class "vega-chart-container"
;;                :ref   ref}
;;          [-vega-chart (cond-> {:spec       (assoc spec
;;                                                   :config default-config)
;;                                :class-name "vega-chart"
;;                                 ;; Turning to JS manually to avoid Reagent converting cebab case to camel case.
;;                                :data       (clj->js data)
;;                                :actions    false
;;                                :width      @width
;;                                :height     height
;;                                :renderer   renderer
;;                                #_#_:tooltip    (.-call (Handler.))}
;;                         (seq signal-handlers)
;;                         (assoc :signal-listeners signal-handlers))]])

;;       :component-did-mount
;;       (fn [_]
;;         (when-let [node (.-current ref)]
;;           (reset! observer
;;                   (doto (js/ResizeObserver.
;;                          (fn [entries _observer]
;;                            (let [^js e (aget entries 0)]
;;                              (reset! width (-> e .-contentRect .-width)))))
;;                     (.observe node)))))

;;       :component-will-unmount
;;       (fn [_]
;;         (when-let [^js obs @observer]
;;           (when-let [node (.-current ref)]
;;             (.unobserve obs node)
;;             (reset! observer nil))))})))

(defn chart
  [_]
  [:p "hi"])
