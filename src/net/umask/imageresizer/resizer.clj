(ns net.umask.imageresizer.resizer
  (:use [clojure.string :only [split join]]
        net.umask.imageresizer.store)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [net.umask.imageresizer.image :as img]
            digest))

(set! *warn-on-reflection* true)

(def ^:const ops [{:key :crop :re-value #"^([0-9]+)x([0-9]+)x([0-9]+)x([0-9]+)$" :keys [:x :y :width :height]}
                  {:key :rotate :re-value #"^([0-9]+)$" :keys [:angle]}
                  {:key :size :re-value #"^([0-9]+)x([0-9]+)$" :keys [:width :height]}
                  {:key :size :re-value #"^([0-9]+)$" :keys [:size]}])

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

(defn- validate-checksum [secretkey uri]
  (let [[checksum rest] (split uri #"/" 2)
        computedchecksum (digest/md5 (str secretkey rest))]
    (= checksum computedchecksum)))


(defn- scale
  [i {size :size width :width height :height}]
  (cond
   (not (nil? size))(img/scale i :size size)
   (not (or (nil? width) (nil? height))) (img/scale i :width width :height height :fit :crop)
   :else i ))

(defn- crop
  [i {x :x y :y width :width height :height}]
  (if (every? (comp not nil?) [x y width height])
    (img/crop i x y width height)
    i))

(defn- rotate
  [img {angle :angle}]
  (if (not (nil? angle))
    (img/rotate img angle)
    img))

(defn- transform [original options]
  (let [bos (java.io.ByteArrayOutputStream.)]
    (-> (img/read original)
        (crop (:crop options))
        (scale (:size options))
        (img/write bos))
    (.toByteArray bos)))

(defn create-ring-handler [secretkey store]
  (fn [request]
    (let [uri (subs (:uri request) 1)
          resizeroptions (:imageresizer request)
          originalname (:original resizeroptions)
          checksum (get-in request [:imageresizer :checksum])
          checksumvalid? (validate-checksum secretkey uri)
          original (store-read store originalname)]
      (if (or (nil? original)
              (not  checksumvalid?))
        {:status 404
         :content-type "text/plain"
         :body (str uri " not valid:" resizeroptions )}
        (let [transformedimage (transform original resizeroptions)]
          (store-write store uri (io/input-stream transformedimage))
          {:status 200
           :content-type "image/jpeg"
           :body (io/input-stream transformedimage)})))))

(defn create-resizer [secretkey store]
  {:store store
   :secretkey secretkey
   :handler (wrap-url-parser  (create-ring-handler secretkey store))})
