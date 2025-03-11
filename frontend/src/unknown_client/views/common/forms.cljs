(ns unknown-client.views.common.forms
    (:require
   [re-frame.core :as rf]
   [re-com.core :as com :refer [at v-box h-box]
    :rename {v-box v
             h-box h}]
   [reagent.core :as r]
   [unknown-client.views.css.forms :as css]))

(defn popover-wrapper
  [{:keys [popover/title
           popover/text
           popover/showing?]}
   component]
  (fn []
    [com/popover-anchor-wrapper
     :src (at)
     :showing?  @showing?
     :position :below-center
     :anchor
     component
     :popover
     [com/popover-content-wrapper
      :src (at)
      :width            "250px"
      :backdrop-opacity 0.3
      :on-cancel        #(reset! showing? false)
      :title            title
      :close-button?    true
      :body             [:span {:style {:color "brown"}} [:p text] ]]]))

(defn ?-icon-button
  [on-click]
  [com/md-icon-button
   :src (at)
   :class (css/help-thingie)
   :md-icon-name "zmdi-help-outline"
   :size :smaller
   :tooltip "What is meant by that?"
   :on-click on-click])

(defn help-thingie
  [{:keys [title text]}]
  (let [showing?       (r/atom nil)]
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
;;     (fn []
;;       [com/popover-anchor-wrapper :src (at)
;;        :showing? (or (:help-buttons @tours/active-tour) showing?)
;;        :position :below-center
;;        :anchor
;;        [com/md-icon-button
;;         :src (at)
;;         :style (when (:help-buttons @tours/active-tour) {:position "relative" :z-index 10})
;;         :class (css/help-thingie)
;;         :md-icon-name "zmdi-help-outline"
;;         :size :smaller
;;         :tooltip "What is meant by that?"
;;         :on-click #(swap! showing? not)]
;;        :popover
;;        [com/popover-content-wrapper
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
   :class (css/checkbox-container)
   :children
   [[com/checkbox
     :src       (at)
     :label     label
     :disabled? disabled?
     :model     model
     :on-change on-change]
    [help-thingie {:title help-title
                   :text  help-text}]]])


(defn info-label
  [label stuff]
  [h :src (at)
     :gap      "4px"
     :children
     [[:span.field-label label]
      [com/info-button
       :src (at)
       :info
       [v :src (at)
        :children
        [stuff]]]]])

(defn browse-files-button
  [on-change]
  [:input {:type      "file"
           :id        "inputGroupFile01"
           :class     "custom-file-input"
           :on-change on-change}])

(defn file-upload
  [file-query
   upload-query]
  (let [file-state (rf/subscribe file-query)
        upload-state (rf/subscribe upload-query)]
    (fn []
      [:div
       [:div {:class "input-group mb-3"}
        [:div {:class "input-group-prepend"}
         [:span {:class "input-group-text"
                 :on-click
                 (when @file-state
                   (fn []
                     (let [form-data (js/FormData.)]
                       (.append form-data "file" @file-state)
                       (rf/dispatch (conj upload-query form-data)))))}
          (if @upload-state "Uploading..." "Upload")]]
        [:div {:class "custom-file"}
         [:input {:type      "file"
                  :id        "inputGroupFile01"
                  :class     "custom-file-input"
                  :on-change
                  (fn [e]
                    (let [files (-> e .-target .-files)]
                      (when (pos? (.-length files))
                        (rf/dispatch (conj file-query (aget files 0))))))}]
         [:label {:for   "inputGroupFile01"
                  :class "custom-file-label"}
          (if @file-state (.-name @file-state) "Choose file")]]]])))
