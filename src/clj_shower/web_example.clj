(ns clj-shower.web_example
    (:use clojure.java.io)
    (:use ring.middleware.file)
    (:use ring.middleware.file-info)
    (:use ring.middleware.reload)
    (:use noir.core)
    (:use hiccup.page)
    (:use hiccup.form)
    (:use hiccup.element) 
    (:import (java.io BufferedReader StringReader)) 
    (:use [clj-shower.core :only [translate]])
    (:require [noir.server :as server]))

(defpage [:get "/"] []
  (with-open [rdr (clojure.java.io/reader "example.shower")]
             (translate (line-seq rdr)))) 

(defn -main [port & args]
  (server/add-middleware wrap-reload 'clj-shower.core)
  (server/add-middleware wrap-file "public")
  (server/add-middleware wrap-file-info)
  (server/start (Integer. port)))


