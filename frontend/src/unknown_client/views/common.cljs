(ns unknown-client.views.common
  (:require
   [re-frame.core :as re-frame]
      [re-com.core :as re-com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [reagent.core :as r]
   [unknown-client.styles :as styles]
   [unknown-client.events :as events]
   [unknown-client.routes :as routes]
   [unknown-client.subs :as subs]
   [unknown-client.tours :as tours]))

(defn link-to-page
  [label path]
  [re-com/hyperlink
   :src      (at)
   :class (styles/inner-page-link)
   :label    label
   :on-click #(re-frame/dispatch (if (coll? path)
                                   (into [::events/navigate]
                                         path)
                                   [::events/navigate path]))])

(defn popover-wrapper
  [{:keys [popover/title
           popover/text
           popover/showing?]}
   component]
  (fn []
    [re-com/popover-anchor-wrapper
     :src (at)
     :showing?  @showing?
     :position :below-center
     :anchor
     component
     :popover
     [re-com/popover-content-wrapper
      :src (at)
      :width            "250px"
      :backdrop-opacity 0.3
      :on-cancel        #(reset! showing? false)
      :title            title
      :close-button?    true
      :body             [:span {:style {:color "brown"}} [:p text] ]]]))

(defn ?-icon-button
  [on-click]
  [re-com/md-icon-button
   :src (at)
   :style (when (:help-buttons @tours/active-tour) {:position "relative" :z-index 10})
   :class (styles/help-thingie)
   :md-icon-name "zmdi-help-outline"
   :size :smaller
   :tooltip "What is meant by that?"
   :on-click on-click])

(defn help-thingie
  [{:keys [title text]}]
  (let [showing?       (r/atom nil)
        cancel-popover #(reset! showing? false)
        #_tour           #_@(re-frame/subscribe [::subs/tour])]
    (fn []
      [popover-wrapper
       {:popover/text text
        :popover/title title
        :popover/showing? showing?}
       [?-icon-button #(swap! showing? not)]])))

;; (defn help-thingie
;;   [{:keys [title text]}]
;;   (let [showing?       (r/atom nil)
;;         cancel-popover #(reset! showing? false)
;;         #_tour           #_@(re-frame/subscribe [::subs/tour])]
;;     (fn []
;;       [re-com/popover-anchor-wrapper :src (at)
;;        :showing? (or (:help-buttons @tours/active-tour) showing?)
;;        :position :below-center
;;        :anchor
;;        [re-com/md-icon-button
;;         :src (at)
;;         :style (when (:help-buttons @tours/active-tour) {:position "relative" :z-index 10})
;;         :class (styles/help-thingie)
;;         :md-icon-name "zmdi-help-outline"
;;         :size :smaller
;;         :tooltip "What is meant by that?"
;;         :on-click #(swap! showing? not)]
;;        :popover
;;        [re-com/popover-content-wrapper
;;         :src (at)
;;         :width            "250px"
;;         :backdrop-opacity 0.3
;;         :on-cancel        cancel-popover
;;         :title            title
;;         :close-button?    true
;;         :body             [:span {:style {:color "brown"}} [:p text] ]]])))

(defn checkbox
  [{:keys [label disabled? model on-change help-title help-text]}]
  [h
   :class (styles/checkbox-container)
   :children
   [[re-com/checkbox
     :src       (at)
     :label     label
     :disabled? disabled?
     :model     model
     :on-change on-change]
    [help-thingie {:title help-title
                   :text  help-text}]]])


(defn navbar-link
  [link-text route]
  (let  [hover?  (r/atom false)]
    (fn []
      [re-com/button
       :src       (at)
       :label    link-text
       :on-click #(re-frame/dispatch
                   (if (coll? route)
                     (into [::events/navigate]
                           route)
                     [::events/navigate route]))
       :class    (styles/inner-page-link)
       :style    {:background-color (if @hover? "#0072bb" "#00796b")}
       :attr     {:on-mouse-over (re-com/handler-fn (reset! hover? true))
                  :on-mouse-out  (re-com/handler-fn (reset! hover? false))}])))
