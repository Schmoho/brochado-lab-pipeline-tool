(ns graph.load.utils
  (:import
   (java.util.concurrent LinkedBlockingQueue))
  (:require [clojure.tools.logging :as log]))

(def thing-doer-registry (atom {}))

(defn make-thing-doer
  "`operation` expects its argument at the end.
  When an output queue is used, you need to specifiy whether the results of
  `operation` should be fed one-by-one or as a collection."
  ([queue operation]
   (let [thread (Thread.
                 (fn do-thing []
                   (while true
                     (try
                       (->> queue (.take) (operation))
                       (catch Throwable t
                         (log/error t))))))]
     (swap! thing-doer-registry
            update
            [queue operation]
            (fnil conj #{}) thread)
     thread))
  ([input-queue output-queue operation]
   (make-thing-doer input-queue output-queue operation {}))
  ([input-queue output-queue operation {:keys [bulk?]}]
   (let [thread (Thread.
                 (fn do-thing []
                   (while true
                     (try
                       (let [result (->> input-queue (.take) (operation))]
                         (when (nil? result) (log/error "Got nil result on" operation))
                         (if-not bulk?
                           (.offer output-queue result)
                           (if-not (seqable? result)
                             (.offer output-queue result)
                             (doseq [r result]
                               (.offer output-queue r)))))
                       (catch Throwable t (log/error t))))))]
     (swap! thing-doer-registry
            update
            [input-queue output-queue operation]
            (fnil conj #{}) thread)
     thread)))

(defn make-download-and-feed-module
  ([download-operation feed-operation]
   (make-download-and-feed-module download-operation feed-operation {}))
  ([download-operation feed-operation {:keys [bulk-download-result?
                                              number-of-feeders
                                              number-of-downloaders]
                                       :as options}]
   (let [download-queue (LinkedBlockingQueue.)
         feed-queue     (LinkedBlockingQueue.)
         downloader     (for [_ (range (or number-of-downloaders 1))]
                          (doto (make-thing-doer
                                 download-queue
                                 feed-queue
                                 download-operation
                                 options)
                            (.start)))
         feeder         (for [_ (range (or number-of-feeders 1))]
                          (doto (make-thing-doer
                                 feed-queue
                                 feed-operation)
                            (.start)))]
     {:download-queue download-queue
      :feed-queue     feed-queue
      :downloader     (vec downloader)
      :feeder         (vec feeder)})))

(defn make-distributor-module
  ([input-operation channel-names]
   (make-distributor-module input-operation channel-names {}))
  ([input-operation channel-names {:keys [bulk-mapping
                                          number-of-input-operators]
                                   :as   options}]
   (let [input-queue    (LinkedBlockingQueue.)
         sort-me-queue  (LinkedBlockingQueue.)
         input-operator (doto (make-thing-doer
                               input-queue
                               sort-me-queue
                               input-operation
                               options)
                          (.start))
         channels       (zipmap channel-names
                                (repeatedly #(LinkedBlockingQueue.)))
         distributor    (doto (make-thing-doer
                               sort-me-queue
                               (fn sort-onto-queues
                                 [sortable]
                                 (doseq [channel-name channel-names]
                                   (let [target-channel (get channels channel-name)
                                         data           (get sortable channel-name)
                                         bulk?          ((or bulk-mapping
                                                             (constantly false))
                                                         channel-name)]
                                     (when data
                                       (if-not bulk?
                                         (.offer target-channel data)
                                         (if-not (seqable? data)
                                           (.offer target-channel data)
                                           (doseq [d data]
                                             (.offer target-channel d)))))))))
                          (.start))]
     {:input-queue    input-queue
      :sort-me-queue  sort-me-queue
      :input-operator input-operator
      :channels       channels
      :distributor    distributor})))

(defn make-connector
  [from to]
  {:connector
   (doto (make-thing-doer
          from
          to
          identity)
     (.start))
   :from from
   :to   to})
