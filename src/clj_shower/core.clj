(ns clj-shower.core
    (:use clojure.pprint)
    (:use clojure.tools.cli)
    (:require [clojure.string :as s])
    (:use hiccup.page)
    (:use hiccup.util)
    (:use hiccup.element))


(def ^:dynamic *parse-as-header* false)

(defn next-slide [lines]
  (take-while (complement s/blank?) lines))

(defn tail-lines
  "Get all lines next to first slide"
  [lines]
  (drop-while s/blank? (drop-while (complement s/blank?) lines)))

(defn lazy-slides-seq [lines]
  "Convert lines seq to slides seq"
  (lazy-seq
    (let [slide (next-slide lines)]
      (if (empty? slide)
        nil
        (cons slide
              (lazy-slides-seq (tail-lines lines)))))))

(defn encode [s]
  (-> s
      (s/replace #"(?<!\\)\*\*([^*]+)\*\*" "<b>$1</b>")
      (s/replace #"(?<!\\)__([^_]+)__"  "<strong>$1</strong>")
      (s/replace #"(?<!\\)\*([^*]+)\*" "<i>$1</i>")
      (s/replace #"(?<!\\)_([^_]+)_"  "<em>$1</em>")
      (s/replace #"(?<!\\)-([^-]+)-"  "<del>$1</del>")
      (s/replace #"\\\*" "*")
      (s/replace #"\\\_" "_")
      ))

(defn encode-code [s]
  (-> s
      (s/replace #"(?<!\\)\*\*([^*]+)\*\*" "<mark class=\"important\">$1</mark>")
      (s/replace #"(?<!\\)\*([^\*]+)\*" "<mark>$1</mark>")
      (s/replace #"\\\*" "*")))

(defn h
  "Make header from line, considering that h1 can be only in the first slide"
  [hashes content]
  (let [level (min 6 (+ (if (true? *parse-as-header*) 0 1) (count hashes)))]
    (with-meta [(keyword (str "h" level)) content] {:title content})))

(defn id [value]
  (with-meta [] {:id value}))

(defn cover [src alt w h]
  (with-meta (image (subs src 1 (dec (count src))) alt) {:class (cons "cover" (remove nil? [w h]))}))

(defn shout [content]
  (with-meta (when-not (empty? content) [:h2 (encode content)]) {:class "shout"})) 

(defn code [content]
  (with-meta [:code (encode-code (escape-html content))] {:wrapper :pre}))

(defn blockquote [content]
  (with-meta [:p (encode content)] {:wrapper :blockquote}))

(defn ul [spaces content]
  (with-meta [:li (encode content)] {:wrapper :ul :indent (count spaces)}))

(defn ol [spaces content]
  (with-meta [:li (encode content)] {:wrapper :ol :indent (count spaces)}))

(def RULES
     [#"shout:\s*(?i)(.*)" shout
      #"cover:\s*(\S+)\s?(\"[^\"]+\")?(\s+w)?(\s+h)?" cover
      #"id:\s*(?i)([a-z0-9_-]+)" id
      #"(#+)\s*(.*)" h
      #"` (.*)" code
      #"> (.*)" blockquote
      #"(\s*)[-*]\s*(.*)" ul
      #"(\s*)\d\.\s*(.*)" ol
      ])

(defn parse-line [line rules]
  (loop [l line
         [re f] (take 2 rules)
         more (drop 2 rules)]
        (if-not re
          [:p (encode line)]
          (if-let [match (re-matches re l)]
            (apply f (next match))
            (recur l (take 2 more) (drop 2 more))))))

(defn convert
  ([parsed-slide-lines]
   (into []
         (lazy-seq
           (when-let [s (seq parsed-slide-lines)]
                     (let [[fst & xs] s
                           {:keys [wrapper indent] :or {indent 0} }(meta fst)]
                       (if wrapper
                         (let [wrapped (take-while #(:wrapper (meta %)) xs)
                               other (drop (count wrapped) xs)]
                           (into
                             [(into [wrapper] (cons fst (convert wrapped indent)))]
                             (convert other)
                             ))
                         (cons fst (convert xs))))))))
  ([parsed-slide-lines prev-indent]
   (lazy-seq
      (when-let [s (seq parsed-slide-lines)]
                (let [[fst & other] s
                      {:keys [wrapper indent] :or {indent 0}} (meta fst)
                      grab? #(>= (get (meta %) :indent 0) indent)
                      grabbed (take-while grab? other)
                      not-grabbed (drop (count grabbed) other) ]
                  (if wrapper
                    (if (> indent prev-indent)
                      (into [[wrapper fst (convert grabbed indent)]]
                            (convert not-grabbed prev-indent))
                      (into [fst] (convert grabbed indent))) 
                    (into [fst] (convert grabbed))))))))

(defn parse-slide-lines
  "Convert slide lines to slide (in Hiccup markup)"
  [slide-lines]
  (let [converted (convert (map #(parse-line % RULES) slide-lines)) 
        slide-meta-data (map meta converted)
        id (first (map :id slide-meta-data))
        classes (->> (map :class slide-meta-data)
                     (remove nil?)
                     flatten
                     distinct
                     (cons "slide")
                     (s/join " "))
        title (first (map :title slide-meta-data))
        attrs {:class classes}
        attrs (if (nil? id) attrs (assoc attrs :id id))]
    (if (true? *parse-as-header*)
      (with-meta (into [:header.caption] (remove empty? converted)) {:title title})
      (into [:section attrs] (remove empty? converted)))))

(defn translate
  "Translate lines to .html"
  [lines]
  (let [[header & other] (lazy-slides-seq lines)
        header (binding [*parse-as-header* true] (parse-slide-lines header))
        other (map parse-slide-lines other)]
    ;(pprint header)
    ;(pprint other)
    (html5
      [:head
       (include-css "/shower/themes/ribbon/styles/screen.css")
       [:title (:title (meta header))]]
      [:body
       (cons header other)
       (include-js "/shower/scripts/script.js") ])
    ))

(defn translate-file
  "Translate .shower to .html"
  [file-name]
  (with-open [rdr (clojure.java.io/reader file-name)]
             (translate (line-seq rdr))))

(defn -main
  "Main function of translator"
  [& args]
  (let [[options args banner] (cli args
                                   ["--shower" ".shower source"]
                                   ["--html" ".html output"])
        {:keys [shower html]} options
        converted (translate-file shower)]
    ;(println converted)
    (with-open [wrtr (clojure.java.io/writer html)]
               (.write wrtr converted))))

