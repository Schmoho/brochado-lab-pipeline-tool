(defproject dasudopit "1.0.0-SNAPSHOT"
  :main schmoho.dasudopit.core
  :uberjar-name "dasudopit.jar"
  ;; this serves to copy the packaged output from Vite for compilation
  ;; :prep-tasks [["run" "-m" "build/copy-dist-to-public"]]
  ;; check out portal https://github.com/djblue/portal?tab=readme-ov-file#demo
  :repl-options {:nrepl-middleware [portal.nrepl/wrap-portal]}
  ;; for downloading the Java BLAST service
  :repositories {"ebi"      {:url "https://www.ebi.ac.uk/uniprot/artifactory/public"}
                 "ebi-repo" {:url "https://www.ebi.ac.uk/~maven/m2repo"}}
  ;; dev-time tooling, probably not relevant in Calva or Cursive
  :plugins [[refactor-nrepl "3.10.0"]
            [cider/cider-nrepl "0.53.0"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.cli "1.1.230"]

                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.5.7"]

                 [ring/ring-core "1.13.0"]
                 [ring/ring-jetty-adapter "1.13.0"]
                 [metosin/reitit "0.8.0-alpha1"]
                 [ring-cors "0.1.13"]

                 ;; mongo db client
                 [com.novemberain/monger "3.6.0"]

                 [clj-http "3.13.0"]

                 ;; JSON parse/write
                 [cheshire "5.13.0"]
                 [org.clojure/data.csv "1.1.0"]
                 ;; convert between CamelCase, snake_case and kebap-case
                 [camel-snake-kebab "0.4.3"]
                 [org.clojure/data.xml "0.0.8"]
                 ;; for xlsx (i.e. excel) files
                 [dk.ative/docjure "1.14.0"]
                 ;; makes reading edn much faster
                 [io.github.tonsky/fast-edn "1.1.2"]
                 ;; codecs, particularly base64
                 [org.clojure/data.codec "0.1.1"]

                 ;; UniProt API, used for BLAST
                 [uk.ac.ebi.uniprot/japi "1.3.3"]]

  :source-paths ["src/backend" "src/common"]
  :resource-paths ["resources/backend"]

  :profiles {:dev {:dependencies [[ring/ring-devel "1.9.5"]
                                  [prone "2021-04-23"]
                                  [djblue/portal "0.58.5"]
                                  [metasoarous/oz "1.6.0-alpha36"]]
                   :source-paths ["env/dev"]
                   :env          {:dev true}
                   :repl-options {:init-ns user}}

             :uberjar {:source-paths ["env/prod"]
                       :env          {:production true}
                       :aot          :all
                       :omit-source  true}}

  :jvm-opts ["-Dslf4j.internal.verbosity=WARN"
             "-Dclojure.tools.logging.factory=clojure.tools.logging.impl/slf4j-factory"
             "-Xmx8G"
             "-Xms1G"])
