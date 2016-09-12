(ns net.umask.imageresizer.watermark
  (:require [net.umask.imageresizer.bufferedimage :refer [buffered-image dimensions]]
            [net.umask.imageresizer.graphics :refer [with-graphics]]
            [net.umask.imageresizer.image :as img]
            [clojure.tools.logging :as log])
  (:import [java.awt.image BufferedImage]))

(defn- center [l1 l2]
  (/ (- l1 l2) 2))

(defn- lookup-coordinates-from-anchor [dest mark anchor]
  (let [{dest-width :width dest-height :height} (dimensions dest)
        {mark-width :width mark-height :height} (dimensions mark)
        xcenter (center dest-width mark-width)
        xright (- dest-width mark-width)
        ycenter (center dest-height mark-height)
        ybottom (- dest-height mark-height)]
    (condp = anchor
      :topleft      [0 0]
      :topcenter    [xcenter 0]
      :topright     [xright 0]
      :midleft      [0 ycenter]
      :midcenter    [xcenter ycenter]
      :midright     [xright ycenter]
      :bottomleft   [0 ybottom]
      :bottomcenter [xcenter ybottom]
      :bottomright  [xright ybottom])))

(defn- watermark-max-dimensions
  [xoffset yoffset image-width image-height]
  [(- image-width xoffset)
   (- image-height yoffset)])

(defn fit-watermark-to-dimensions [wmark width height]
  (let [{wmark-height :height wmark-width :width} (dimensions wmark)]
    (log/debug "fit watermark (" wmark-width "," wmark-height ") to (" width "," height ")")
    (if (or (> wmark-height height)
            (> wmark-width  width))
      (img/scale wmark :mode :auto :width width :height height)
      wmark)))

(defn- watermark-x-y [img wmark x y]
  (let [{wmark-height :height wmark-width :width} (dimensions wmark)
        {  img-height :height   img-width :width} (dimensions img)]
    (with-graphics (buffered-image img)
      (.drawImage (fit-watermark-to-dimensions wmark
                                               (- img-width x)
                                               (- img-height y))
                  x y nil))))

(defn- watermark-anchor [img wmark anchor]
  (let [{wmark-height :height wmark-width :width} (dimensions wmark)
        {  img-height :height   img-width :width} (dimensions img)
        wmark (fit-watermark-to-dimensions wmark img-width img-height)
        [x y] (lookup-coordinates-from-anchor img wmark anchor)]
    (watermark-x-y img wmark x y)))

(defn watermark
  ([dest mark options]
   (if (contains? options :anchor)
     (watermark-anchor dest mark (:anchor options))
     (watermark-x-y dest mark (:x options) (:y options)))))
