(ns net.umask.imageresizer.urlparser
  (:require [clojure.edn :as edn]
            [clojure.string :refer [join split] :as string]
            [clojure.tools.logging :refer [debug trace]]
            [net.umask.imageresizer.util :refer [trim-leading-slash]]))

(set! *warn-on-reflection* true)

(derive ::x          ::int)
(derive ::y          ::int)
(derive ::width      ::int)
(derive ::height     ::int)
(derive ::size       ::int)
(derive ::angle      ::int)
(derive ::color      ::hex)
(derive ::anchor     ::keyword)
(derive ::expires-at ::long)
(derive ::quality    ::int)
(derive ::format     ::keyword)

(defn- parseInt
  ([x] (Integer/parseInt x))
  ([x r] (Integer/parseInt x r)))

(defn- parseLong [x]
  (Long/parseLong x))

(defmulti read-value  (fn [[k v]] (keyword (namespace ::int) (name k))))

(defmethod read-value ::int [[k v]] [k (parseInt v)])

(defmethod read-value ::long [[k v]] [k (parseLong v)])

(defmethod read-value ::hex [[k v]] [k (parseInt v 16)])

(defmethod read-value ::keyword [[k v]] [k (keyword v)])

(defmethod read-value :default [[k v]] [k v])

(def ^:const ops [{:key :output
                   :re-value #"^(png|jpg)$"
                   :keys [:format]}
                  {:key :output
                   :re-value #"^(jpg)-([0-9]{1,3})$"
                   :keys [:format :quality]}
                  {:key :crop
                   :re-value #"^([0-9]+)x([0-9]+)x([0-9]+)x([0-9]+)$"
                   :keys [:x :y :width :height]}
                  {:key :rotate
                   :re-value #"^([0-9]+)$"
                   :keys [:angle]}
                  {:key :size
                   :re-value #"^([0-9]+)x([0-9]+)$"
                   :keys [:width :height]}
                  {:key :size
                   :re-value #"^([0-9]+)x([0-9]+)-0x([0-9A-Fa-f]{6})$"
                   :keys [:width :height :color]}
                  {:key :size
                   :re-value #"^([0-9]+)$"
                   :keys [:size]}
                  {:key :size
                   :re-value #"^([0-9]+)w$"
                   :keys [:width]}
                  {:key :size
                   :re-value #"^([0-9]+)h$"
                   :keys [:height]}
                  {:key :watermark
                   :re-value #"^(-?[0-9]+)x(-?[0-9]+)-([0-9A-Za-z.]+)$"
                   :keys [:x :y :watermark]}
                  {:key :watermark
                   :re-value #"^((?:top|mid|bottom)(?:left|center|right))-([0-9A-Za-z.]+)$"
                   :keys [:anchor :watermark]}])



(defn option-from-url [uri {option :key valuepattern :re-value valuekeys :keys}]
  (let [[key value rest] (string/split (trim-leading-slash uri) #"/" 3)]
    (if-let [values (and
                     (= key (name option))
                     (re-find valuepattern value))]
      (into {:uri (str "/" rest)} (map read-value (zipmap valuekeys values)))
      {:uri uri})))

(defn- option-map [op key value]
  (if (= (name (:key op)) key)
    (let [values (re-find (:re-value op) value)
          options (into {} (map read-value (zipmap (:keys op) (rest values))))]
      (if (empty? options)
        {}
        {(:key op) options}))
    {}))

(defn- parse-url-into-options [uri-components]
  (trace "parse-url-into-options components " uri-components)
  (loop [ops ops
         urlparts uri-components
         result {:uri (join "/" urlparts)}]
    (if (or  (empty? ops)
             (< (count urlparts) 3))
      (merge result {:original (join "/" urlparts)})
      (let [op (first ops)
            map (apply option-map op (take 2 urlparts))]
        (recur (rest ops)
               (if (empty? map) urlparts (nthrest urlparts 2))
               (merge result map))))))

(defn- parse-url-request [request]
  (let [uri (:uri request)
        [empty & uri-components] (split uri #"/")
        resizer-map (parse-url-into-options uri-components)]
    (assoc request :imageresizer resizer-map)))

(defn wrap-url-parser [handler]
  (fn [request]
    (-> request
        parse-url-request
        handler)))
