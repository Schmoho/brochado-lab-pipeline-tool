(ns channels
  (:require [clojure.core.async :as a :refer [<!! >!! <! >!]]))

;; Use chan to make an unbuffered channel:

(a/chan)

;; Pass a number to create a channel with a fixed buffer size:

(a/chan 10)

;; close! a channel to stop accepting puts. Remaining values are still available to take. Drained channels return nil on take. Nils may not be sent over a channel explicitly!

(let [c (a/chan)]
  (a/close! c))

;; Use `dropping-buffer` to drop newest values when the buffer is full:
(a/chan (a/dropping-buffer 10))

;; Use `sliding-buffer` to drop oldest values when the buffer is full:
(a/chan (a/sliding-buffer 10))


(let [c (a/chan 10)]
  (>!! c "hello")
  (assert (= "hello" (<!! c)))
  (a/close! c))

;; Because these are blocking calls, if we try to put on an unbuffered channel, we will block the main thread.
;; We can use thread (like future) to execute a body in a pool thread and return a channel with the result.
;; Here we launch a background task to put "hello" on a channel, then read that value in the current thread.

(let [c (a/chan)]
  (a/thread (>!! c "hello"))
  (assert (= "hello" (<!! c)))
  (a/close! c))



(let [c (a/chan)]
  (a/go (>! c "hello"))
  (assert (= "hello" (<!! (a/go (<! c)))))
  (a/close! c))

;; (a/go-loop []
;;   (<! (a/timeout 100))
;;   (println "Hello from process 1")
;;   (recur))

;; (a/go-loop []
;;   (<! (a/timeout 250))
;;   (println "Hello from process 2")
;;   (recur))
