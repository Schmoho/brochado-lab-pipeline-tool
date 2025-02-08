(defproject structure-analysis "1.0.0-SNAPSHOT"
  :main core
  :profiles {:uberjar {:aot :all}}
  :plugins [[refactor-nrepl "3.10.0"]
            [cider/cider-nrepl "0.52.0"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.cli "1.1.230"]
                 
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.5.7"]
                 
                 [com.github.full-spectrum/neo4clj-core "1.1.0"]
                 [com.novemberain/monger "3.6.0"]
                 #_[com.yetanalytics/flint "0.3.0"]
                 
                 [clj-http "3.13.0"]
                 
                 [cheshire "5.13.0"]
                 [clj-commons/clj-yaml "1.0.29"]
                 [org.clojure/data.csv "1.1.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [dk.ative/docjure "1.14.0"]]
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"])

