(ns graph.mapping.utils
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.tools.logging :as log]))

(defn remove-nils
  [m]
  (cond
    (and m (map? m))
    (->> m
         (filter (fn [[k v]]
                   (when-not (nil? v)
                     [k v])))
         (into {}))
    (and (not (map-entry? m))
         (or (seq? m)
             (vector? m))) (filter some? m)
    :else m))

(defn stringify-id
  [m]
  (if (and m (map? m) (some? (:id m))) 
    (update m :id str)
    m))

(defn escape-backticks
  [s]
  (if (and s (string? s)) 
    (str/replace s "'" "\\'")
    s))

;; (defn sanitize
;;   [neo4j-input]
;;   (walk/postwalk
;;    (comp
;;     remove-nils
;;     stringify-id
;;     escape-backticks)
;;    neo4j-input))

(defn sanitize
  [m]
  (reduce-kv
   (fn [acc k v]
     #_(let [v (cond
               (nil? v)    ""
               (string? v) (escape-backticks v)
               :else       v)
           k (cond
               (= :id k) (stringify-id k)
               :else     k)]
         (assoc acc k v))
     (if (nil? v)
       acc
       (let [v (cond
                 (string? v) (escape-backticks v)
                 :else       v)
             k (cond
                 (= :id k) (stringify-id k)
                 :else     k)]
         (assoc acc k v))))
   {}
   m))

(defn pascal-case-keyword
  [s]
  (->> (str/split s #"[\ -]")
       (map
        (fn uppercase-first-letter
          [s]
          (str (str/upper-case (subs s 0 1))
               (subs s 1))))
       (apply str)
       keyword))

(defn sanitize-ref-id
  [s]
  (if (and s (string? s)) 
    (str/replace s #"[\\.\-\\:|]" "_")
    s))

(defn sanitize-graph
  [g]
  {:lookups (some->> g :lookups (map #(update % :props sanitize)) set)
   :nodes (some->> g :nodes (map #(update % :props sanitize)) set)
   :rels  (some->> g :rels (map #(update % :props sanitize)) set)
   :returns (some->> g :returns (map sanitize-ref-id) set)})

(defn rel-ref-id
  [a b]
  (sanitize-ref-id (str "REL_" a "__" b)))

(defn rel-between
  [type a b]
  {:type   type
   :ref-id (rel-ref-id
            (:ref-id a)
            (:ref-id b))
   :from   {:ref-id (:ref-id a)
            :props  {:id (-> a :props :id)}}
   :to     {:ref-id (:ref-id b)
            :props  {:id (-> b :props :id)}}})

(defn merge-graphs
  [& gs]
  {:lookups (doall (distinct (apply concat (map :lookups gs))))
   :nodes   (doall (distinct (apply concat (map :nodes gs))))
   :rels    (doall (distinct (apply concat (map :rels gs))))
   :returns (doall (distinct (apply concat (map :returns gs))))})
