(ns unknown-client.views.common.structure
  (:require
   [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [reagent.core :as r]
   [unknown-client.views.css.structure :as css]))

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
