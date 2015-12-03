(ns net.umask.imageresizer.watermark-test
  (:require [clojure.test :refer :all]
            [net.umask.imageresizer.bufferedimage :refer [dimensions]]
            [net.umask.imageresizer.watermark :refer [watermark]]
                        [net.umask.imageresizer.testutils :refer [generate-test-image]])
  (:import java.awt.Color))

(defn- find-watermark [img]
  (let [black (.getRGB Color/BLACK)
        {width :width height :height} (dimensions img)]
    (->> (for [x (range 0 width)
               y (range 0 height)]
           {:x  x :y y :rgb (.getRGB img x y)})
         (filter #(= black (:rgb %1)))
         (reduce #(assoc! %1
                         :x1 (or (:x1 %1) (:x %2))
                         :y1 (or (:y1 %1) (:y %2))
                         :x2 (max (get %1 :x2 0) (:x %2))
                         :y2 (max (get %1 :y2 0) (:y %2)))
                 (transient  {}))
         (persistent!)
         (#(vector (:x1 %1) (:y1 %1) (:x2 %1) (:y2 %1))))))

(defn- add-watermark-and-find [options]
  (let [img (generate-test-image 20 40 :white)
        wimg (generate-test-image 10 10 :black)]
    (find-watermark (watermark img wimg options))))

(deftest test-watermark
  (are [expected options] (= expected (add-watermark-and-find options))
    [0   0  9  9] {:x 0 :y 0}
    [1   1 10 10] {:x 1 :y 1}
    [0   0  9  9] {:anchor :topleft}
    [5   0 14  9] {:anchor :topcenter}
    [10  0 19  9] {:anchor :topright}
    [0  15  9 24] {:anchor :midleft}
    [5  15 14 24] {:anchor :midcenter}
    [10 15 19 24] {:anchor :midright}
    [0  30  9 39] {:anchor :bottomleft}
    [5  30 14 39] {:anchor :bottomcenter}
    [10 30 19 39] {:anchor :bottomright}))
