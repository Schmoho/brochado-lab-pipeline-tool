(ns args
  (:require
   [fast-edn.core :as edn]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  ;; An option with a required argument
  [["-c" "--config-file FILE" "Config file"
    :parse-fn #(edn/read-string (slurp %))]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Unknown Server"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn parse-args
  "Parse command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or the options."
  [args]
  (let [{:keys [options arguments errors summary] :as parsed} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else ; failed custom validation => exit with usage summary
      parsed)))
