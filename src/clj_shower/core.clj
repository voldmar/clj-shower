(ns clj-shower.core
    (:use clojure.tools.cli)
    (:use clojure.pprint)
    (:use clojure.string)
    (:use hiccup.page)
    (:use hiccup.element))

(def ^:dynamic *parse-as-header* false)

(defn next-slide [lines]
  (take-while (complement blank?) lines))

(defn tail-lines [lines]
  (drop-while blank? (drop-while (complement blank?) lines)))

(defn lazy-slides-seq [lines]
  (lazy-seq
    (let [slide (next-slide lines)]
      (if (empty? slide)
        nil
        (cons slide
              (lazy-slides-seq (tail-lines lines)))))))

(defn h [line]
  (let [[hashes contents] (rest (re-matches #"(#+)\s*(.*)" line))
        level (min 6 (+ (if (true? *parse-as-header*) 0 1) (count hashes)))]
    (with-meta [(keyword (str "h" level)) contents] {:title contents})))

(defn id [line]
  (let [[contents] (first (rest (re-matches #"id:\s+(?i)([a-z0-9_-]+)" line)))]
    (with-meta [] {:id contents})))

(defn cover [line]
  (let [[src alt w h] (rest (re-matches #"cover:\s+(\S+)\s?(\"[^\"]+\")?\s?(w)?\s?(h)?" line))]
    (with-meta (image src alt) {:class (cons "cover" (remove nil? [w h]))})))

(defn shout [line]
  (let [[contents] (first (rest (re-matches #"shout:\s+(?i)(.*)" line)))]
    (with-meta (when-not (empty? contents) [:h2 contents]) {:class "shout"}))) 

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

(defn parse-slide [slide]
  (let [parsed (map parse-line slide)
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
  "Translate .shower to .html"
  [lines]
  (let [[header & other] (lazy-slides-seq lines)
        header (binding [*parse-as-header* true] (parse-slide header))
        other (map parse-slide other)]
    (pprint header)
    (html5
      [:head
       [:title (:title (meta header))]] 
      [:body
       (cons header other)])))

(defn translate-file
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

