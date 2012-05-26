(defproject clj-shower "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.trace "0.7.3"]
                 [hiccup "1.0.0"]
                 [noir "1.3.0-beta3"]]
  :main clj-shower.core
  :run-aliases {:web clj-shower.web}
  :eval-in :subprocess
  :plugins [[lein-swank "1.4.4"]
            [lein-marginalia "0.7.0"]
            [lein-reload "1.0.0"]]
  :jvm-opts ["-Dfile.encoding=UTF-8"])

