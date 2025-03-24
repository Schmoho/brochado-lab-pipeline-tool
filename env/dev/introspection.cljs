(ns introspection)


;; (def all-global-keys (js->clj (js/Object.getOwnPropertyNames js/window)))

;; (def global-keys (js->clj (js/Object.keys js/window)))
;; (def props (js/Object.getOwnPropertyNames js/$3Dmol))

;; (js->clj props)

;; (vec (for [prop props]
;;    (let [val (aget js/$3Dmol prop)]
;;      [prop (type val)])))
