(ns net.umask.imageresizer.resizer
  (:use [clojure.string :only [split join]]
        net.umask.imageresizer.store
        net.umask.imageresizer.urlparser
        [clojure.tools.logging :only [info debug]]
        [ring.util.response :only [response not-found header content-type]])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [net.umask.imageresizer.image :as img]
            digest)
  (:import [java.awt.image BufferedImage]
           [java.awt Graphics Color]))

(defmulti scale (fn [image size] (if-not (nil? size) (keys size))))

(defmethod scale '(:size) [image {size :size}]
  (img/scale image :size size :fit :auto :method :ultra-quality :ops [:antialias]))

(defmethod scale '(:color :height :width) [image {height :height width :width color :color}]
  (let [img-width (.getWidth image)
        img-height (.getHeight image)
        fit (if (>= (/ height width) (/ img-height img-width))
              :width
              :height)
        resized-image (img/scale image
                                 :width width
                                 :height height :fit fit :method :ultra-quality :ops [:antialias])
        color (Color. color)
        new-image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        graphics (.getGraphics new-image)
        resized-image-height (.getHeight resized-image)
        resized-image-width  (.getWidth resized-image)
        x (/ (- width resized-image-width) 2)
        y (/ (- height resized-image-height) 2)]
    (do (doto graphics
          (.setColor  color)
          (.fillRect 0 0 width height)
          (.drawImage resized-image x y nil)
          (.dispose))
        new-image)))

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

(defn- fill-alpha [img]
  (let [width (.getWidth img)
        height (.getHeight img)
        type (.getType img)]
    (if (= BufferedImage/TYPE_INT_ARGB type)
      (let [new-image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
            graphics (.getGraphics new-image)]
        (do  (doto graphics
               (.setColor Color/WHITE)
               (.fillRect 0 0 width height)
               (.drawImage img 0 0 nil)
               (.dispose))
             new-image))
      img)))

(defn- transform [^java.io.InputStream original options]
  (let [bos (java.io.ByteArrayOutputStream.)]
    (try 
      (-> (img/read original)
          (crop (:crop options))
          (scale (:size options))
          (fill-alpha)
          (img/write bos :quality 90))
      (finally (.close original)))
    (.toByteArray bos)))

(defn create-ring-handler [store]
  (fn [request]
    (let [uri (subs (:uri request) 1)
          resizeroptions (:imageresizer request)
          originalname (:original resizeroptions)
          original (store-read store originalname)
          store? (not= originalname (:uri resizeroptions))] ;; dont store the original again, causes quality degradation
      (if (nil? original)
        (do
          (info "image not found or checksum not valid for uri" uri)
          (-> (not-found (str "original file " originalname " not found"))
              (content-type "text/plain")))
        (let [transformedimage (transform original resizeroptions)]
          (when store?
            (debug "storing image " (:uri resizeroptions) " to storage")
            (store-write store (:uri resizeroptions) (io/input-stream transformedimage)))
          (-> (response (io/input-stream transformedimage))
              (content-type "image/jpeg")
              (header "Content-Length" (alength transformedimage))))))))

(defn create-resizer [secret store]
  {:store store
   
   :handler (wrap-url-parser secret (create-ring-handler store))})
