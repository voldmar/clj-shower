(ns clj-shower.test.core
  (:use [clj-shower.core])
  (:use [clojure.test])
  (:use [hiccup.page]))

(deftest simple-ul
  (is (= (html5 (ul ["* one" "* two" "* three"]))
         ("<ul><li>one</li><li>two</li><li>three</li></ul>"))))
