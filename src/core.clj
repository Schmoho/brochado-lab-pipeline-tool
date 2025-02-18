(ns core
  (:gen-class)
  (:require
   [args :as args]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [graph.accrete.core :as accrete]
   [server.core :as server]))

(def config (atom nil))

(defn exit [status msg]
  (log/info msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [action options exit-message ok?]} (args/parse-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        (log/info "Running with config:" (:config-file options))
        (or (:config-file options)
            (edn/read-string (slurp (io/resource "default-config.edn"))))
        (accrete/start! accrete/system)
        (server/start!)))))

