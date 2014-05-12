(ns net.umask.imageresizer.resizer
  (:use [clojure.string :only [split join]]
        net.umask.imageresizer.store
        net.umask.imageresizer.urlparser
        [clojure.tools.logging :only [info debug]])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [net.umask.imageresizer.image :as img]
            digest))

(defmulti scale (fn [image size] (if-not (nil? size) (keys size))))

(defmethod scale '(:size) [image {size :size}]
  (img/scale image :size size :fit :auto :method :ultra-quality :ops [:antialias]))

(defmethod scale '(:height :width) [image {width :width height :height}]
  (img/scale image :width width :height height :fit :crop :method :ultra-quality :ops [:antialias]))

(defmethod scale '(:height) [image {height :height}]
  (img/scale image :size height :fit :height :method :ultra-quality :ops [:antialias]))

(defmethod scale '(:width) [image {width :width}]
  (debug "scaling to width " width)
  (img/scale image :size width :fit :width :method :ultra-quality :ops [:antialias]))

(defmethod scale nil [image size] image)

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

(defn- transform [^java.io.InputStream original options]
  (let [bos (java.io.ByteArrayOutputStream.)]
    (try 
      (-> (img/read original)
          (crop (:crop options))
          (scale (:size options))
          (img/write bos :quality 100))
      (finally (.close original)))
    (.toByteArray bos)))

(defn create-ring-handler [store]
  (fn [request]
    (let [uri (subs (:uri request) 1)
          resizeroptions (:imageresizer request)
          originalname (:original resizeroptions)
          original (store-read store originalname)]
      (if (nil? original)
        (do
          (info "image not found or checksum not valid for uri" uri)
          {:status 404
           :content-type "text/plain"
           :body ""})
        (let [transformedimage (transform original resizeroptions)]
          (do  (store-write store (:uri resizeroptions) (io/input-stream transformedimage))
               {:status 200
                :content-type "image/jpeg"
                :body (io/input-stream transformedimage)}))))))

(defn create-resizer [secret store]
  {:store store
   
   :handler (wrap-url-parser secret (create-ring-handler store))})
