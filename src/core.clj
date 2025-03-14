(ns core
  (:gen-class)
  (:require
   [args :as args]
   [fast-edn.core :as edn]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
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
        (let [input-config (or (:config-file options)
                               (edn/read-string (slurp (io/resource "default-config.edn"))))]
          (log/info "Running with config:" input-config)
          (reset! config input-config)
          (server/start! (:http-port @config)))))))

