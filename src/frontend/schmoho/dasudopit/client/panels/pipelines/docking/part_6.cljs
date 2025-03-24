(ns schmoho.dasudopit.client.panels.pipelines.docking.part-6
  (:require
   [re-com.core :as com :rename {h-box h, v-box v}]
   [schmoho.dasudopit.client.common.views.forms :as form-views]
   [schmoho.dasudopit.client.common.views.vega :as vega]))


(defn part-6
  [proteins]
  [v
   :children
   [[:h4 {:style {:color "darkred"}} "Dummy data for illustration"]
    [:div
     [form-views/action-button
      :label "Upload"]]
    [h
     :children
     [[vega/chart {:spec
                   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                    :data {:name :test}
                    :description "A simple bar chart with embedded data."
                    :mark "bar"
                    :encoding {:x {:field "a" :type "nominal" :axis {:labelAngle 0}}
                               :y {:field "b" :type "quantitative"}}}
                   :data {:test [{:a "A" :b 27}
                                 {:a "B" :b 55}
                                 {:a "C" :b 43}
                                 {:a "D" :b 91}
                                 {:a "E" :b 81}
                                 {:a "F" :b 53}
                                 {:a "G" :b 19}
                                 {:a "H" :b 87}
                                 {:a "I" :b 52}]}}]
      [:img {:src "https://www.ebi.ac.uk/thornton-srv/software/LigPlus/manual2/ligplot_3tmn.gif"
             :style {:width "300px"
                     :height "auto"}}]]]]])
