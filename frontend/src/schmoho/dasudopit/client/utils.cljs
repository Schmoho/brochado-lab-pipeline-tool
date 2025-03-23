(ns schmoho.dasudopit.client.utils
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as rf.db]
   [schmoho.dasudopit.client.common.http :as http]))

(defn get-data
  [path]
  (when-not (get (@rf.db/app-db :queries) path)
    (rf/dispatch [::http/http-get path])))

(defn cool-select-keys
  [m kaccessors]
  (->> kaccessors
       (map
        (fn [[new-key kaccessor]]
          [new-key (if (coll? kaccessor)
                     (reduce (fn [acc kac]
                               (kac acc))
                             m
                             kaccessor)
                     (kaccessor m))]))
       (into {})))


(defn rand-str
  []
  (apply str (repeatedly 20 #(rand-nth "abcdefghijklmnopqrstuvwxyz"))))


(defn protein-go-terms
  [protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(= "GO" (:database %)))
       (map (fn [go-term]
              {:id (:id go-term)
               :label (->> (:properties go-term)
                           (filter #(= "GoTerm" (:key %)))
                           first
                           :value)}))))

(defn proteome-go-terms
  [proteome]
  (->> proteome
      (mapcat protein-go-terms)
      set))

(defn has-go-term?
  [go-term protein]
  (->> protein
       :uniProtKBCrossReferences
       (filter #(and (= "GO" (:database %))
                     (= go-term (:id %))))
       not-empty))

(defn go-term-filtering-fn
  [proteome go-term]
  (let [proteome-lookup
        (->> proteome
             (map (juxt :primaryAccession identity))
             (into {}))]
    (fn [table-row]
      (some->> table-row
               :protein_id
               proteome-lookup
               (has-go-term? go-term)))))

(defn protein-feature->location
  [protein-feature]
  [(-> protein-feature :location :start :value)
   (-> protein-feature :location :end :value)])



;; (def all-global-keys (js->clj (js/Object.getOwnPropertyNames js/window)))

;; (def global-keys (js->clj (js/Object.keys js/window)))
;; (def props (js/Object.getOwnPropertyNames js/$3Dmol))

;; (js->clj props)

;; (vec (for [prop props]
;;    (let [val (aget js/$3Dmol prop)]
;;      [prop (type val)])))

