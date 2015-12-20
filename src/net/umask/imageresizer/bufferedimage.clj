(ns net.umask.imageresizer.bufferedimage
  (:import [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [java.net URL]
           [java.io InputStream]))

(def ^:const typemapping
  {:rgb BufferedImage/TYPE_INT_RGB})

(defn dimensions [^BufferedImage img]
  {:width (.getWidth img)
   :height (.getHeight img)})

(defn new-buffered-image
  ([x y] (new-buffered-image x y :rgb))
  ([x y space] (BufferedImage. x y (get typemapping space BufferedImage/TYPE_INT_RGB))))

(defprotocol ToBufferedImage
  (^BufferedImage buffered-image [this]))


(extend-protocol ToBufferedImage
  java.awt.image.BufferedImage
  (buffered-image [this] this)
  java.io.InputStream
  (buffered-image [this] (try (ImageIO/read this)
                              (finally (.close this))))
  java.net.URL
  (buffered-image [this] (ImageIO/read this))
  java.lang.String
  (buffered-image [this] (buffered-image (URL. this)))
  nil
  (buffered-image [this] (new-buffered-image 1 1)))


