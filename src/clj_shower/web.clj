(ns clj-shower.web
    (:use clojure.java.io)
    (:use ring.middleware.file)
    (:use ring.middleware.file-info)
    (:use noir.core)
    (:use hiccup.page)
    (:use hiccup.form)
    (:use hiccup.element) 
    (:import (java.io BufferedReader StringReader)) 
    (:use [clj-shower.core :only [translate]])
    (:require [noir.server :as server]))

(defpage [:get "/"] []
  (html5
    (form-to [:post "/"]
      (text-area {:cols 80 :rows 30} "text")
      (submit-button "Generate shower")
      ))) 

(defpage [:post "/"] {:keys [text]}
  (translate (line-seq (BufferedReader. (StringReader. text)))))

(defn -main [port & args]
  ;(server/add-middleware wrap-file "static")
  ;(server/add-middleware wrap-file-info)
  (server/start (Integer. port)))


