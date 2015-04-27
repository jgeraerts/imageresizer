(ns net.umask.imageresizer.resizer
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug warn]]
            [net.umask.imageresizer.cache :refer [wrap-cache]]
            [net.umask.imageresizer.checksum :refer [wrap-checksum]]
            [net.umask.imageresizer.image :as img]
            [net.umask.imageresizer.source :refer [get-image-stream]]
            [net.umask.imageresizer.urlparser :refer :all]
            [net.umask.imageresizer.bytebuffer :refer [wrap-bytebuffer]]
            [ring.util.response :refer [content-type header not-found
                                        response]])
  (:import (java.awt Color)
           (java.awt.image BufferedImage)))

(defmulti scale (fn [image size] (if-not (nil? size) (keys size))))

(defmethod scale '(:size) [image {size :size}]
  (img/scale image :size size :fit :auto :method :ultra-quality :ops [:antialias]))

(defmethod scale '(:color :height :width) [^BufferedImage image {height :height width :width color :color}]
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
    (doto graphics
      (.setColor  color)
      (.fillRect 0 0 width height)
      (.drawImage resized-image x y nil)
      (.dispose))
    new-image))

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
  (if-not (nil? angle)
    (img/rotate img angle)
    img))

(defn- fill-alpha [^BufferedImage img]
  (let [width (.getWidth img)
        height (.getHeight img)
        type (.getType img)]
    (if (= BufferedImage/TYPE_INT_ARGB type)
      (let [new-image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
            graphics (.getGraphics new-image)]
        (doto graphics
          (.setColor Color/WHITE)
          (.fillRect 0 0 width height)
          (.drawImage img 0 0 nil)
          (.dispose))
        new-image)
      img)))

(defn- transform #^bytes [^java.io.InputStream original options]
  (let [bos (java.io.ByteArrayOutputStream.)]
    (try 
      (-> (img/read original)
          (crop (:crop options))
          (scale (:size options))
          (fill-alpha)
          (img/write bos :quality 90))
      (finally (.close original)))
    (.toByteArray bos)))

(defn create-ring-handler [source]
  (fn [request]
    (let [uri (subs (:uri request) 1)
          resizeroptions (:imageresizer request)
          originalname (:original resizeroptions)
          original (get-image-stream source originalname)] 
      (if (nil? original)
        (do
          (warn "image not found for uri" uri)
          (-> (not-found (str "original file " originalname " not found"))
              (content-type "text/plain")))
        (let [transformedimage (transform original resizeroptions)]
          
          (-> (response transformedimage)
              (content-type "image/jpeg")
              (header "Content-Length" (alength transformedimage))))))))

(defn create-resizer [secret source cache]
  {:store source
   :handler (->> (create-ring-handler source)
                (wrap-cache cache)
                (wrap-url-parser)
                (wrap-checksum secret)
                (wrap-bytebuffer))})
