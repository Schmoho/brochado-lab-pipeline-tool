(ns schmoho.components.forms
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [reagent.core :as r]
   [schmoho.components.css.forms :as css]
   [schmoho.utils.csv :as csv]))

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
  [label on-change-fn]
  [:div
   [:div {:class "input-group mb-3"}
    [:div {:class "custom-file"}
     [:input {:type  "file"
              :id    "inputGroupFile01"
              :class "custom-file-input"
              :on-change
              (fn [e]
                (let [files (-> e .-target .-files)]
                  (on-change-fn files)))}]
     [:label {:for   "inputGroupFile01"
              :class "custom-file-label"}
      (or label "Choose file")]]]])

(defn csv-upload
  [& {:keys [on-load label]}]
  [file-upload
   label
   #(doseq [file (array-seq %)]
      (if-not (str/ends-with? (.-name file) ".csv")
        (js/alert "Can only handle CSV data.")
        (let [reader (js/FileReader.)]
          (set! (.-onload reader)
                (fn [e]
                  (let [csv-text (.-result reader)
                        data     (csv/parse-csv-with-header
                                  csv-text
                                  :numeric-fields ["log_transformed_f_statistic"
                                                   "fdr"
                                                   "effect_size"])]
                    (on-load data))))
          (.readAsText reader file))))])

(defn input-text
  [& {:keys [on-change placeholder model attr required? info-body label style width]
      :or   {required? true
             label     "Name"
             info-body [:<>]}}]
  [v
   :children
   [[info-label
     (str (if required?
            "Required: "
            "Optional: ")
          label)
     info-body]
    [com/input-text
     :model model
     :on-change #(do (when on-change (on-change %)))
     :placeholder placeholder
     :attr attr
     :style style
     :width width]]])

(defn action-button
  [& {:keys [label on-click style disabled?]
      :or {style {:width "200px"}
           disabled? false}}]
  (let [hover? (r/atom false)]
    (fn [& {:keys [label on-click style disabled?]
            :or {style {:width "200px"}
                 disabled? false}}]
      [com/button
       :src      (at)
       :disabled? disabled?
       :label    label
       :class    (css/rectangle-button)
       :on-click #(when on-click
                    (on-click %))
       :style    (merge {:background-color
                         (if @hover? "#0072bb" "#4d90fe")}
                        style)
       :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
                  :on-mouse-out  (com/handler-fn (reset! hover? false))}])))

(defn dropdown
  [& {:keys [choices on-change placeholder model info-body label style width required?]
      :or   {required? true
             label     ""
             info-body [:<>]}}]
  (let [model (or model (r/atom nil))]
    [v
     :children
     [[info-label
       (str (if required?
              "Required: "
              "Optional: ")
            label)
       info-body]    
      [com/single-dropdown
       :model model
       :choices choices
       :on-change #(do
                     (reset! model %)
                     (on-change %))
       :placeholder placeholder]]]))


(defn table
  [data & {:keys [columns on-enter-row on-leave-row]}]
  (if (nil? @data)
    [com/throbber :size :regular]
    [v
     :width "100%"
     :children
     [[h
       :children
       [[com/simple-v-table
         :src                       (at)
         :model data
         :columns
         (mapv (fn [defaults input]
                 (merge defaults input))
               (map (fn [col]
                      (assoc
                       {:width 250
                        :align "center"
                        :vertical-align "middle"}
                       :row-label-fn #((:id col) %)
                       :header-label (name (:id col))))
                    columns)
               columns)
         :row-height                35
         :on-enter-row on-enter-row
         :on-leave-row on-leave-row]]]]]))

(defn alert
  [& {:keys [heading body dismissible? alert-type]}]
  [:div {:class (str "alert fade show "
                     (case alert-type
                       :warning "alert-warning "
                       :danger "alert-danger "
                       :success "alert-success "
                       :primary "alert-primary "
                       :secondary "alert-secondary "
                       :info "alert-info "
                       "alert-info ")
                     (when dismissible? "alert-dismissible "))
         :role "alert"}
   [:strong heading]
   body
   [:button {:type "button"
             :class "close"
             :data-dismiss "alert"
             :aria-label "Close"}
    [:span {:aria-hidden "true"} "×"]]])


(defn eliding-label
  [label]
  (if (< 50 (count label))
    [:h6 (str (subs label 0 50) "...")]
    [:h6 (str label)]))

(defn pill-badge
  [& {:keys [label true?]}]
  [h
   :children
   [[:span {:class "badge badge-secondary"} label]
    (if true?
      [:i {:class "zmdi zmdi-check-circle"
           :style {:color "green"}}]
      [:i {:class "zmdi zmdi-block"
           :style {:color "red"}}])]])
