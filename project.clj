(defproject unknown "1.0.0-SNAPSHOT"
  :main core
  :uberjar-name "unknown.jar"
  :plugins [[refactor-nrepl "3.10.0"]
            [cider/cider-nrepl "0.52.0"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/tools.cli "1.1.230"]
                 
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.5.7"]

                 [ring/ring-core "1.13.0"]
                 [ring/ring-jetty-adapter "1.13.0"]
                 [metosin/reitit "0.8.0-alpha1"]
                 
                 [com.github.full-spectrum/neo4clj-core "1.1.0"]
                 [com.novemberain/monger "3.6.0"]
                 #_[com.yetanalytics/flint "0.3.0"]
                 
                 [clj-http "3.13.0"]
                 
                 [cheshire "5.13.0"]
                 [clj-commons/clj-yaml "1.0.29"]
                 [org.clojure/data.csv "1.1.0"]
                 [camel-snake-kebab "0.4.3"]
                 [org.clojure/data.xml "0.0.8"]
                 [dk.ative/docjure "1.14.0"]]

  :source-paths ["src"]
  :resource-paths ["resources"]

  :profiles {:dev {:dependencies [[ring/ring-mock "0.4.0"]
                                  [ring/ring-devel "1.9.5"]
                                  [prone "2021-04-23"]]
                   
                   :source-paths ["env/dev"]
                   
                   :env          {:dev true}
                   :repl-options {:init-ns user}}

             :uberjar {:source-paths ["env/prod"]
                       :env          {:production true}
                       :aot          :all
                       :omit-source  true}}
  
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"
             "-Xmx8G"
             "-Xms2G"])
