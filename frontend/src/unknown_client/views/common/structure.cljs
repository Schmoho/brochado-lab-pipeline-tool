(ns unknown-client.views.common.structure
  (:require
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [reagent.core :as r]
   [unknown-client.views.css.structure :as css]
   [unknown-client.utils :as utils]))

(defn card
  [header title body & {:keys [on-click]}]
  (let [hover?  (r/atom false)]
    (fn []
      [:div {:class (str "card bg-light mb-3 "
                         (if @hover? (css/card-hover) ""))
             :style {:width "42rem"}
             :on-mouse-over (re-com/handler-fn (reset! hover? true))
             :on-mouse-out  (re-com/handler-fn (reset! hover? false))
             :on-click on-click}
       [:div {:class "card-header"} header]
       [:div {:class "card-body"
              :style {:font-size "16px"}}
        [:h5 {:class "card-title"} title]
        body]])))

(defn clickable-card
  [header title body & {:keys [on-click]}]
  (let [hover?  (r/atom false)]
    (fn []
      [:div {:class (str "card bg-light mb-3 "
                         (if @hover? (css/clickable-card-hover) ""))
             :style {:width "42rem"}
             :on-mouse-over (re-com/handler-fn (reset! hover? true))
             :on-mouse-out  (re-com/handler-fn (reset! hover? false))
             :on-click on-click}
       [:div {:class "card-header"} header]
       [:div {:class "card-body"
              :style {:font-size "16px"}}
        [:h5 {:class "card-title"} title]
        body]])))

(defn collapsible-card
  [heading body & {:keys [expanded? data-parent id]}]
  (let [id            (or id (utils/rand-str))
        heading-id    id
        aria-controls (str id "-aria")]
    [:div {:class "card"}
     [:div {:class         "card-header",
            :id            heading-id
            :data-toggle   "collapse"
            :data-target   (str "#" aria-controls)
            :aria-expanded expanded?
            :aria-controls aria-controls}
      [:h5 {:class "mb-0"}
       heading]]
     [:div {:id              aria-controls
            :class           "collapse show",
            :aria-labelledby heading-id
            :data-parent     data-parent}
      [:div {:class "card-body"}
       body]]]))

(defn collapsible-accordion
  [& cards]
  (let [accordion-id (utils/rand-str)]
    (->> (for [card cards :when (some? card)]
           ^{:key (first card)} [collapsible-card (first card) (second card) :data-parent (str "#" accordion-id)])
         (into [:div {:class "accordion",
                      :id accordion-id
                      :style {:width "1200px"}}]))))

(defn collapsible-accordion-2
  [& cards]
  (let [accordion-id (utils/rand-str)]
    (->> (for [card cards :when (some? card)]
           ^{:key (first card)} [collapsible-card (first card) (second card)])
         (into [:div {:class "accordion",
                      :id accordion-id
                      :style {:width "1200px"}}]))))
