(ns channels
  (:import
   (java.util.concurrent LinkedBlockingQueue))
  (:require [clojure.tools.logging :as log]))

(defn make-sink!
  [{:keys [input-channel operation]}]
  (doto (Thread.
         (fn sink-thing []
           (while true
             (try
               (if (instance? java.util.Queue input-channel)
                 (->> input-channel (.take) (operation))
                 (operation
                  (for [q input-channel]
                    (->> q (.take)))))
               (catch InterruptedException e
                 (log/info "Thread was interrupted."))
               (catch Throwable t
                 (log/error t))))))
    (.start)))

(defn make-thing-doer!
  "`operation` expects its argument at the end.
  When an output channel is used, you need to specifiy whether the results of
  `operation` should be fed one-by-one or as a collection."
  ([input-channel output-channel operation]
   (make-thing-doer! input-channel output-channel operation {}))
  ([input-channel output-channel operation {:keys [bulk?]}]
   (doto (Thread.
          (fn do-thing []
            (while true
              (try
                (let [result  (if (instance? java.util.Queue input-channel)
                                (->> input-channel (.take) (operation))
                                (operation
                                 (for [q input-channel]
                                   (->> q (.take)))))                             ]
                  (when (nil? result) (log/error "Got nil result on" operation))
                  (if-not bulk?
                    (.offer output-channel result)
                    (if-not (sequential? result)
                      (.offer output-channel result)
                      (doseq [r result]
                        (.offer output-channel r)))))
                (catch InterruptedException e
                  (log/info "Thread was interrupted."))
                (catch Throwable t (log/error t))))))
     (.start))))

(defn make-linear-module!
  [{:keys [operation
           bulk?
           number-of-operators
           input-channel
           output-channel]
    :as   options}]
  (let [input-channel  (or input-channel (LinkedBlockingQueue.))
        output-channel (or output-channel (LinkedBlockingQueue.))
        operators    (for [_ (range (or number-of-operators 1))]
                       (make-thing-doer!
                        input-channel
                        output-channel
                        operation
                        options))]
    {:input-channel  input-channel
     :output-channel output-channel
     :operators    operators}))

(defn make-distributor-module!
  [{:keys [operation
           channel-names
           channels
           input-channel
           bulk-mapping
           number-of-input-operators]
    :as   options}]
  (let [input-channel   (or input-channel (LinkedBlockingQueue.))
        sort-me-channel (LinkedBlockingQueue.)
        input-operator  (make-thing-doer!
                         input-channel
                         sort-me-channel
                         operation
                         options)
        channels        (or channels
                            (zipmap channel-names
                                    (repeatedly #(LinkedBlockingQueue.))))
        distributor     (make-sink!
                         {:input-channel sort-me-channel
                          :operation
                          (fn sort-onto-channels
                            [sortable]
                            (doseq [channel-name (keys channels)]
                              (let [target-channel (get channels channel-name)
                                    data           (get sortable channel-name)
                                    bulk?          ((or bulk-mapping
                                                        (constantly false))
                                                    channel-name)]
                                (when data
                                  (if-not bulk?
                                    (.offer target-channel data)
                                    (if-not (sequential? data)
                                      (.offer target-channel data)
                                      (doseq [d data]
                                        (.offer target-channel d))))))))})]
    {:input-channel   input-channel
     :sort-me-channel sort-me-channel
     :input-operator  input-operator
     :channels        channels
     :distributor     distributor}))

(defn make-funnel!
  [{:keys [from to]}]
  (let [to        (or to (LinkedBlockingQueue.))
        funnelers (for [f from]
                    (make-thing-doer!
                     f
                     to
                     identity))]
    {:funnel funnelers
     :from   from
     :to     to}))


(defn pipe!
  [{:keys [from to]}]
  {:pipe (make-thing-doer!
          from
          to
          identity)
   :from from
   :to   to})
