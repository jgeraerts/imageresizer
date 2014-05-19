(ns net.umask.imageresizer.image_test
  (:require [net.umask.imageresizer.image :as i]
            [clojure.java.io :as io])
  (:use clojure.test)
  (:import javax.imageio.ImageIO ))

(deftest resize
  (let [rose (ImageIO/read (io/input-stream (io/resource "rose.jpg")))
        resized1 (i/scale rose :width 1500 :height 100 :fit :crop)
        resized2 (i/scale rose :width  100 :height 1500 :fit :crop)]
    (is (= [1500 100] [(.getWidth resized1) (.getHeight resized1)]))
    (is (= [100 1500] [(.getWidth resized2) (.getHeight resized2)]))))