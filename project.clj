(defproject clj-shower "1.0.4"
  :description "Quick .shower to .html converter"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [hiccup "1.0.2"]]
  :main clj-shower.core
  :run-aliases {:web clj-shower.web
                :web-example clj-shower.web_example}
  :eval-in :subprocess
  :jvm-opts ["-Dfile.encoding=UTF-8"])

