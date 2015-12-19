(ns net.umask.imageresizer.resizer
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug warn]]
            [net.umask.imageresizer.bufferedimage :refer [buffered-image new-buffered-image dimensions]]
            [net.umask.imageresizer.bytebuffer :refer [wrap-bytebuffer]]
            [net.umask.imageresizer.cache :refer [wrap-cache]]
            [net.umask.imageresizer.checksum :refer [wrap-checksum]]
            [net.umask.imageresizer.expires :refer [wrap-expires]]
            [net.umask.imageresizer.graphics :refer [with-graphics]]
            [net.umask.imageresizer.image :as img]
            [net.umask.imageresizer.source :refer [get-image-stream]]
            [net.umask.imageresizer.urlparser :refer :all]
            [net.umask.imageresizer.watermark :refer [watermark]]
            [ring.util.response :refer [content-type header not-found
                                        response]])
  (:import (java.awt Color)
           (java.awt.image BufferedImage)))

(def ^:const content-types {:jpg "image/jpeg"
                            :png "image/png"})

(def ^:const default-output-format {:format :jpg :quality 90})

(defn- all-not-nil? [ vals ]
  (every? (comp not nil?) vals))

(defmulti scale (fn [_ size] (into #{} (keys size))))

(defmethod scale #{:size} [i {size :size}]
  (img/scale i :size size :fit :auto :method :ultra-quality :ops [:antialias]))

(defmethod scale #{:color :height :width} [^BufferedImage i {height :height width :width color :color}]
  (debug "scaling to (" width "," height ") color " color)
  (let [{img-width :width img-height :height} (dimensions i)
        fit (if (>= (/ height width) (/ img-height img-width))
              :width
              :height)
        resized-image (img/scale i
                                 :width width
                                 :height height :fit fit :method :ultra-quality :ops [:antialias])
        {resized-image-height :height resized-image-width :width} (dimensions resized-image)
        x (/ (- width resized-image-width) 2)
        y (/ (- height resized-image-height) 2)]
    (with-graphics (new-buffered-image width height :rgb)
                  (.setColor  (Color. color))
                  (.fillRect 0 0 width height)
                  (.drawImage resized-image x y nil))))

(defmethod scale #{:height :width} [i {width :width height :height}]
  (img/scale i :width width :height height :fit :crop :method :ultra-quality :ops [:antialias]))

(defmethod scale #{:height} [i {height :height}]
  (img/scale i :size height :fit :height :method :ultra-quality :ops [:antialias]))

(defmethod scale #{:width} [i {width :width}]
  (debug "scaling to width " width)
  (img/scale i :size width :fit :width :method :ultra-quality :ops [:antialias]))

(defmethod scale :default [image size] image)

(defn- crop
  [i {x :x y :y width :width height :height}]
  (if (all-not-nil? [x y width height])
    (img/crop i x y width height)
    i))

(defn- rotate
  [i {angle :angle}]
  (if-not (nil? angle)
    (img/rotate i angle)
    i))

(defn- fill-alpha [^BufferedImage i]
  (let [{width :width height :height} (dimensions i)
        type (.getType i)]
    (if (= BufferedImage/TYPE_INT_ARGB type)
      (with-graphics (new-buffered-image width height :rgb)
        (.setColor Color/WHITE)
        (.fillRect 0 0 width height)
        (.drawImage i 0 0 nil))
      i)))

(defn- addwatermark
  [^BufferedImage i watermarksource {watermarkname :watermark :as options}]
  (if-let [watermark-img (and (all-not-nil? [watermarkname watermarksource])
                              (get-image-stream watermarksource watermarkname))]
    (watermark i (buffered-image watermark-img) options)
    i))

(defn- wrap-transform [trans-fn handler]
  (fn [request]
    (let [{:keys [body status] :as response} (handler request)]
      (if (= 200 status)
        (assoc response :body (trans-fn body request))
        response))))

(defn wrap-output [handler]
  (fn [request]
    (let [{:keys [body status] :as response} (handler request)]
      (if (= 200 status)
        (let [{:keys [format quality] :as options} (get-in request [:imageresizer :output] default-output-format)
              bos (java.io.ByteArrayOutputStream.)]
          (img/write body bos :format format :quality quality)
          (-> response
              (assoc :body (.toByteArray bos))
              (header "Content-Type" (content-types format))))
        response))))

(defn wrap-fill [handler]
  (wrap-transform (fn [body request] (fill-alpha body)) handler))

(defn wrap-scale [handler]
  (wrap-transform (fn [body request] (scale body (get-in request [:imageresizer :size]))) handler))

(defn wrap-crop [handler]
  (wrap-transform (fn [body request] (crop body (get-in request [:imageresizer :crop]))) handler))

(defn wrap-watermark [watermarks handler]
  (wrap-transform (fn [body request ]
                    (addwatermark body
                                  watermarks
                                  (get-in request [:imageresizer :watermark]))) handler))

(defn load-source [source]
  (fn [request]
    (let [source-image-name (get-in request [:imageresizer :original])
          source-image      (get-image-stream source source-image-name)]
      (if-not (nil? source-image)
        (response (buffered-image source-image))
        (do
          (warn "image not found for uri" (:uri request))
          (-> (not-found (str "original file " source-image-name " not found"))
              (content-type "text/plain")))))))


(defn create-resizer [secret originals watermarks cache]
  {:handler (->> (load-source originals)
                 (wrap-watermark watermarks)
                 (wrap-crop)
                 (wrap-scale)
                 (wrap-fill)
                 (wrap-output)
                 (wrap-cache cache)
                 (wrap-expires)
                 (wrap-url-parser)
                 (wrap-checksum secret)
                 (wrap-bytebuffer))})
