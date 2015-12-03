(ns net.umask.imageresizer.testutils
  (:require [net.umask.imageresizer.bufferedimage :refer [new-buffered-image]]
            [net.umask.imageresizer.graphics :refer [with-graphics]])
  (:import java.awt.Color))

(def colors {:white Color/WHITE
             :red   Color/RED
             :black Color/BLACK})

(defn generate-test-image
  ([width height]
   (generate-test-image width height :white))
  ([width height color]
   (let [b (new-buffered-image width height)]
     (with-graphics b
       (.setColor (color colors))
       (.fillRect 0 0 width height )))))
