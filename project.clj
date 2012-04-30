(defproject clj-shower "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [hiccup "1.0.0"]] 
  :dev-dependencies [[lein-marginalia "0.7.0"]]
  :main clj-shower.core
  :eval-in :subprocess
  :plugins [[lein-swank "1.4.4"]]
  :jvm-opts ["-Dfile.encoding=UTF-8"])

