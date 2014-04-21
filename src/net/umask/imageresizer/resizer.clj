(ns net.umask.imageresizer.resizer
  (:use [clojure.string :only [split join]])
  (:require [clojure.edn :as edn]))

(def ^:const ops [{:key :crop :re-value #"^([0-9]+)x([0-9]+)x([0-9]+)x([0-9]+)$" :keys [:x :y :width :height]}
                  {:key :rotate :re-value #"^([0-9]+)$" :keys [:angle]}
                  {:key :size :re-value #"^([0-9]+)x([0-9]+)$" :keys [:width :height]}
                  {:key :size :re-value #"^([0-9]+)$" :keys [:targetsize]}])
                  

(defn create-resizer []
  {})

(defn- option-map [op key value]
  (if (= (name (:key op)) key)
    (let [values (re-find (:re-value op) value)
          map (zipmap (:keys op) (map edn/read-string (rest values)))]
      (if (empty? map)
        {}
        {(:key op) map}))
    {}))

(defn- parse-url-into-options [url]
  (if-let [[empty checksum & urlparts] (split url #"/")]
    (loop [ops ops
           urlparts urlparts
           result {:checksum checksum}]
      (if (or  (empty? ops)
               (< (count urlparts) 3))
        (merge result {:original (join "/" urlparts)})
        (let [op (first ops)
              map (apply option-map op (take 2 urlparts))]
          (recur (rest ops)
                 (if (empty? map) urlparts (nthrest urlparts 2))
                 (merge result map)))))))

(defn- parse-url-request [request]
  (let [url (:uri request)
        resizer-map (parse-url-into-options url)]
    (assoc request :imageresizer resizer-map)))

(defn wrap-url-parser [handler]
  (fn [request]
    (-> request
        parse-url-request
        handler)))
