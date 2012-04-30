(ns clj-shower.core
    (:use clojure.tools.cli)
    (:use clojure.pprint)
    (:use clojure.string)
    (:use hiccup.page)
    (:use hiccup.element))

(def ^:dynamic *parse-as-header* false)

(defn next-slide [lines]
  (take-while (complement blank?) lines))

(defn tail-lines
  "Get all lines next to first slide"
  [lines]
  (drop-while blank? (drop-while (complement blank?) lines)))

(defn lazy-slides-seq [lines]
  "Convert lines seq to slides seq"
  (lazy-seq
    (let [slide (next-slide lines)]
      (if (empty? slide)
        nil
        (cons slide
              (lazy-slides-seq (tail-lines lines)))))))

(defn h
  "Make header from line, considering that h1 can be only in the first slide"
  [line]
  (let [[hashes content] (rest (re-matches #"(#+)\s*(.*)" line))
        level (min 6 (+ (if (true? *parse-as-header*) 0 1) (count hashes)))]
    (with-meta [(keyword (str "h" level)) content] {:title content})))

(defn id [line]
  (let [[content] (first (rest (re-matches #"id:\s+(?i)([a-z0-9_-]+)" line)))]
    (with-meta [] {:id content})))

(defn cover [line]
  (let [[src alt w h] (rest (re-matches #"cover:\s+(\S+)\s?(\"[^\"]+\")?\s?(w)?\s?(h)?" line))]
    (with-meta (image src alt) {:class (cons "cover" (remove nil? [w h]))})))

(defn shout [line]
  (let [[content] (first (rest (re-matches #"shout:\s+(?i)(.*)" line)))]
    (with-meta (when-not (empty? content) [:h2 content]) {:class "shout"}))) 

(defn p [line]
  [:p line])

(defn collect [key maps]
  (set (flatten (map key maps))))

(defn parse-line [line]
  (cond
    (.startsWith line "#") (h line)
    (.startsWith line "id:") (id line)
    (.startsWith line "cover:") (cover line)
    (.startsWith line "shout:") (shout line)
    :else (p line)
    ))

(defn parse-slide-lines
  "Convert slide lines to slide (in Hiccup markup)"
  [slide-lines]
  (let [parsed (map parse-line slide-lines)
        slide-meta-data (map meta parsed)
        id (first (map :id slide-meta-data))
        classes (->> (map :class slide-meta-data)
                     (remove nil?)
                     flatten
                     distinct
                     (cons "slide")
                     (join " "))
        title (first (map :title slide-meta-data))
        attrs {:class classes}
        attrs (if (nil? id) attrs (assoc attrs :id id))]
    (with-meta [:section attrs (filter not-empty parsed)] {:title title})))

(defn translate
  "Translate lines to .html"
  [lines]
  (let [[header & other] (lazy-slides-seq lines)
        header (binding [*parse-as-header* true] (parse-slide-lines header))
        other (map parse-slide-lines other)]
    (html5
      [:head
       [:title (:title (meta header))]]
      [:body
       (cons header other)])))

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
        {:keys [shower html]} options]
    (println (translate-file shower))))

