(ns schmoho.components.utils.vega)

(defn clicked-points-signal
  [signal-value]
  (-> (js->clj signal-value)
      (get "vlPoint")
      (get "or")
      (->> (map (fn [a] (get a "_vgsid_"))))))
