(ns net.umask.imageresizer.watermark
  (:require [net.umask.imageresizer.bufferedimage :refer [buffered-image dimensions]]
            [net.umask.imageresizer.graphics :refer [with-graphics]]))

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

(defn- lookup-coordinates [ dest mark options]
  (if (contains? options :anchor)
    (lookup-coordinates-from-anchor dest mark (:anchor options))
    [(:x options) (:y options)]))


(defn watermark
  ([dest mark options]
   (apply (partial watermark dest mark) (lookup-coordinates dest mark options))
   dest)
  ([dest mark ^long x ^long  y]
   (with-graphics (buffered-image  dest)
     (.drawImage  (buffered-image mark) x y nil))))

