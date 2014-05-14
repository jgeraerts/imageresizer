(ns net.umask.imageresizer.urlparser
  (:use [clojure.string :only [join split]]
        [clojure.tools.logging :only [warn trace]])
  (:require [clojure.edn :as edn]
            digest))

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
         urlparts (rest uri-components)
         result {:checksum (first uri-components)
                 :uri      (join "/" urlparts)}]
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

(defn- if-checksum-valid? [request secret handler]
  (trace  "if-checksum-valid? request:" request)
  (let [checksum (get-in request [:imageresizer :checksum])
        remaining-uri (get-in request [:imageresizer :uri])]
    (if (= checksum (digest/md5 (str secret remaining-uri))) 
      (handler request)
      ( do (warn "invalid checksum " checksum " for request " (:uri request))
           {:status 400}))))

(defn wrap-url-parser [secret handler]
  (fn [request]
    (-> request
        parse-url-request
        (if-checksum-valid? secret handler))))

