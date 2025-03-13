(ns unknown-client.views.common.forms
  (:require
   [clojure.string :as str]
   [goog.labs.format.csv :as csv]
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

(defn file-upload
  [on-change-fn]
  (let [file-state (r/atom nil)]
    (fn []
      (prn @file-state)
      [:div
       [:div {:class "input-group mb-3"}
        [:div {:class "custom-file"}
         [:input {:type  "file"
                  :id    "inputGroupFile01"
                  :class "custom-file-input"
                  :on-change
                  (fn [e]
                    (let [files (-> e .-target .-files)]
                      (reset! file-state files)
                      (on-change-fn files)))}]
         [:label {:for   "inputGroupFile01"
                  :class "custom-file-label"}
          (if @file-state (.-name (aget @file-state 0)) "Choose file")]]]])))

(defn parse-csv-with-header
  [csv-text & {:keys [numeric-fields]}]
  (let [rows   (csv/parse csv-text)      ;; returns a vector of vectors (rows)
        header (first rows)]
    (->> (map #(zipmap header %) (rest rows))
         (mapv (fn [row]
                 (reduce
                  (fn [row field]
                    (update row field #(if (str/includes? % ".")
                                         (parse-double %)
                                         (parse-long %))))
                  row
                  numeric-fields))))))

(defn csv-upload
  [& {:keys [on-load]}]
  [file-upload
   #(doseq [file (array-seq %)]
      (if-not (str/ends-with? (.-name file) ".csv")
        (js/alert "Can only handle CSV data.")
        (let [reader (js/FileReader.)]
          (set! (.-onload reader)
                (fn [e]
                  (let [csv-text (.-result reader)
                        data     (parse-csv-with-header
                                  csv-text
                                  :numeric-fields ["log_transformed_f_statistic"
                                                   "fdr"
                                                   "effect_size"])]
                    (on-load data))))
          (.readAsText reader file))))])


(defn input-text
  [& {:keys [on-change placeholder attr]}]
  (let [model (r/atom nil)]
    (fn []
      [com/input-text
       :model model
       :on-change #(do
                     (reset! model %)
                     (when on-change (on-change %)))
       :placeholder placeholder
       :attr attr])))

(defn action-button
  [& {:keys [label on-click]}]
  (let [hover? (r/atom false)]
    (fn []
      [com/button
       :src      (at)
       :label    label
       :class    (css/rectangle-button)
       :style    {:background-color "#0072bb"}
       :on-click on-click
       :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
       :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                  :on-mouse-out  (com/handler-fn (reset! hover? false))}])))

(defn dropdown
  [& {:keys [choices on-change placeholder model]}]
  (let [model (or model (r/atom nil))]
    [com/single-dropdown
     :model model
     :choices choices
     :on-change #(do
                   (reset! model %)
                   (on-change %))
     :placeholder placeholder]))
