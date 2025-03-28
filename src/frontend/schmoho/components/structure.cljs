(ns schmoho.components.structure
  (:require
   [re-com.core :as com :refer [at] :rename {v-box v h-box h}]
   [reagent.core :as r]
   [schmoho.components.css.structure :as css]
   [schmoho.utils.string :as utils.str]))

(defn minicard
  [header body]
  [:div {:class "card bg-light mb-3 "
         :style {:font-size "14px"
                 :height "100%"}}
   (when header
     [:div {:class "card-header"
           :style {:font-size "14px"}} header])
   [:div {:class "card-body"
          :style {:font-size "14px"}}
    body]])

(defn card
  [& {:keys [on-click width
             header title body]
    :or {width "42rem"}}]
   (let [hover?  (r/atom false)]
     (fn []
       [:div {:class (str "card bg-light mb-3 "
                          (if @hover? (css/card-hover) ""))
              :style {:width width}
              :on-mouse-over (com/handler-fn (reset! hover? true))
              :on-mouse-out  (com/handler-fn (reset! hover? false))
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
             :on-mouse-over (com/handler-fn (reset! hover? true))
             :on-mouse-out  (com/handler-fn (reset! hover? false))
             :on-click on-click}
       [:div {:class "card-header"} header]
       [:div {:class "card-body"
              :style {:font-size "16px"}}
        [:h5 {:class "card-title"} title]
        body]])))

(defn collapsible-card
  [heading body & {:keys [expanded? data-parent id]}]
  (let [id            (or id (utils.str/rand-str))
        heading-id    id
        aria-controls (str id "-aria")]
    [:div {:class "card bg-light"}
     [:div {:class         "card-header",
            :id            heading-id
            :data-toggle   "collapse"
            :data-target   (str "#" aria-controls)
            :aria-expanded expanded?
            :aria-controls aria-controls}
      [:h6 {:class "mb-0"}
       heading]]
     [:div {:id              aria-controls
            :class           "collapse show",
            :aria-labelledby heading-id
            :data-parent     data-parent}
      [:div {:class "card-body"}
       body]]]))

(defn collapsible-accordion
  [& cards]
  (let [accordion-id (utils.str/rand-str)]
    (->> (for [card cards :when (some? card)]
           ^{:key (first card)}
           [collapsible-card
            (first card)
            (second card)
            :data-parent
            (str "#" accordion-id)])
         (into [:div {:class "accordion",
                      :id accordion-id
                      :style {:width "1200px"}}]))))

(defn collapsible-accordion-2
  [& cards]
  (let [accordion-id (utils.str/rand-str)]
    (->> (for [card cards :when (some? card)]
           ^{:key (first card)} [collapsible-card
                                 (first card)
                                 (second card)])
         (into [:div {:class "accordion",
                      :id accordion-id
                      :style {:width "100%"}}]))))

(defn header
  [& {:keys [label]}]
  [com/title
   :src   (at)
   :label label
   :level :level1
   :style {:color "white"}])


(defn carousel
  [slides]
  (let [id "some"]
    [:div {:id            "some"
           :class         "carousel slide"
           :data-ride     "carousel"
           :data-interval false}
     (into [:ol {:class "carousel-indicators"}]
           (->> slides
                (map-indexed
                 (fn [idx _]
                   [:li {:data-target   (str "#" id)
                         :data-slide-to (str idx)
                         :class         (when (= 0 idx) "active")
                         :style {:background-color "#787878"}}]))))
     (into [:div {:class "carousel-inner"}]
           (->> slides
                (map-indexed (fn [idx slide]
                               [:div (if (= 0 idx)
                                       {:class "carousel-item active"}
                                       {:class "carousel-item"}) slide]))))
     (when (< 1 (count slides))
       [:a {:class      "carousel-control-prev"
            :href       (str "#" id)
            :role       "button"
            :data-slide "prev"}
        [:span {:class       (css/my-carousel-control-prev-icon)
                :aria-hidden "true"}]
        [:span {:class "sr-only"} "Previous"]])
     (when (< 1 (count slides))
       [:a {:class      "carousel-control-next"
            :href       (str "#" id)
            :role       "button"
            :data-slide "next"}
        [:span {:class       (css/my-carousel-control-next-icon)
                :aria-hidden "true"}]
        [:span {:class "sr-only"} "Next"]])]))

(defn flex-horizontal-center
  [component]
  [h
   :children
   [[com/gap :size "1"]
    component
    [com/gap :size "1"]]])

(defn flex-horizontal-right
  [component]
  [h
   :children
   [[com/gap :size "1"]
    component]])

(defn vertical-bar-tabs
  [& {:keys [model tabs on-change style id-fn label-fn]}]
  (let [buttons (->> tabs
                     (map (fn [stuff]
                            [:button
                             {:type "button"
                              :style style
                              :class "btn btn-secondary"
                              :on-click #(on-change (id-fn stuff))}
                             (subs (label-fn stuff) 0 30)])))]
    (into [:div {:class "btn-group-vertical"
                 :style style}]
          buttons)))
