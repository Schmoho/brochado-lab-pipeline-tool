(ns schmoho.dasudopit.client.panels.pipelines.docking.choose-binding-sites
  (:require
   [clojure.string :as str]
   [re-com.core :as com :refer [at] :rename {h-box h, v-box v}]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [schmoho.dasudopit.client.forms :as forms]
   [schmoho.dasudopit.client.http :as http]
   [schmoho.components.css.forms :as css]))


;; (defn handle-get-structures-click-fn
;;   [selected-proteins selected-taxons]
;;   (fn []
;;     (doseq [t selected-taxons]
;;       (rf/dispatch [::forms/set-form-data
;;                     :docking
;;                     :input-model
;;                     :taxon
;;                     t
;;                     :plddt-cutoff
;;                     80]))))

;; (defn get-structures-button
;;   []
;;   (let [hover? (r/atom false)]
;;     (fn []
;;       (let [input-model       @(rf/subscribe [:forms.docking/input-model])
;;             selected-proteins (->> input-model
;;                                    :taxon
;;                                    vals
;;                                    (map (comp :id :protein)))
;;             selected-taxons   (->> input-model :taxon keys)]
;;         (when (every? some? selected-proteins)
;;           [com/button
;;            :src       (at)
;;            :label    "GET STRUCTURES"
;;            :class    (css/rectangle-button)
;;            :style    {:background-color "#0072bb"}
;;            :on-click (handle-get-structures-click-fn selected-proteins selected-taxons)
;;            :style    {:background-color (if @hover? "#0072bb" "#4d90fe")}
;;            :attr     {:on-mouse-over (com/handler-fn (reset! hover? true))
;;                       :on-mouse-out  (com/handler-fn (reset! hover? false))}])))))

(defn choose-binding-sites-form
  []
  ;; (let [form-valid?        @(rf/subscribe [:forms.docking.choose-binding-sites/valid?])]
  ;;   [v
  ;;    :children
  ;;    [(when-not form-valid?
  ;;       [:span "Please choose a protein for each taxon and press the button to get the structures."])
  ;;     [h
  ;;      :min-height "300px"
  ;;      :gap "30px"
  ;;      :children
  ;;      (into [] proteome-searchers)]
  ;;     [get-structures-button]]])
  #_[get-structures-button]
  [:p "hi"])
