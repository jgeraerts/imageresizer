(ns net.umask.imageresizer.urlparser
  (:require [clojure.edn :as edn]
            [clojure.string :refer [join split]]
            [clojure.tools.logging :refer [trace]]))

(set! *warn-on-reflection* true)

(def ^:const ops [{:key :crop :re-value #"^([0-9]+)x([0-9]+)x([0-9]+)x([0-9]+)$" :keys [:x :y :width :height]}
                  {:key :rotate :re-value #"^([0-9]+)$" :keys [:angle]}
                  {:key :size :re-value #"^([0-9]+)x([0-9]+)$" :keys [:width :height]}
                  {:key :size :re-value #"^([0-9]+)x([0-9]+)-(0x[0-9A-Fa-f]{6})$" :keys [:width :height :color]}
                  {:key :size :re-value #"^([0-9]+)$" :keys [:size]}
                  {:key :size :re-value #"^([0-9]+)w$" :keys [:width]}
                  {:key :size :re-value #"^([0-9]+)h$" :keys [:height]}])

(defn- option-map [op key value]
  (if (= (name (:key op)) key)
    (let [values (re-find (:re-value op) value)
          map (zipmap (:keys op) (map edn/read-string (rest values)))]
      (if (empty? map)
        {}
        {(:key op) map}))
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

