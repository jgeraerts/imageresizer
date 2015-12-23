(ns net.umask.imageresizer.image
  (:refer-clojure :exclude [read])
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :refer [debug trace]]
            [net.umask.imageresizer.graphics :refer [with-graphics]]
            [net.umask.imageresizer.bufferedimage :refer [buffered-image dimensions new-buffered-image]]
            [net.umask.imageresizer.util :refer [assert-larger-than-zero]]
            [slingshot.slingshot :refer [throw+]])
  (:import [org.imgscalr Scalr Scalr$Method Scalr$Mode]
           [java.awt Image Transparency Color]
           [java.io OutputStream InputStream]
           [java.awt.image BufferedImage RenderedImage BufferedImageOp]
           [javax.imageio ImageIO ImageWriter ImageWriteParam IIOImage]
           [javax.imageio.stream FileImageOutputStream FileCacheImageOutputStream]
           [com.mortennobel.imagescaling ResampleOp]))

(set! *warn-on-reflection* true)

(defn round-ratio [^Double x] (Math/round x))

(defn calculate-dimensions-width
  "calculate target dimensions prefering width"
  [origWidth origHeight requestedWidth requestedHeight]
  (let [ratio (/  origWidth origHeight)
        targetHeight (round-ratio (/ requestedWidth ratio))]
    [requestedWidth targetHeight]))

(defn calculate-dimensions-height
  "calculate target demensions preferring height"
  [origWidth origHeight requestedWidth requestedHeight]
  (let [ratio (/ origWidth origHeight)
        targetWidth (round-ratio (* requestedHeight ratio))]
    [targetWidth requestedHeight ]))

(defn calculate-dimensions-auto
  "calculate target dimensions based on orientation"
  [origWidth origHeight requestedWidth requestedHeight]
  (let [ratio (/ origWidth origHeight)]
    (if (>= ratio 1)
      [requestedWidth (round-ratio (/ requestedWidth ratio))]
      [(round-ratio (* requestedHeight ratio)) requestedHeight])))

(def ^:const fits {:height calculate-dimensions-height
                   :width calculate-dimensions-width
                   :auto calculate-dimensions-auto})

(defn- ^BufferedImage resize
  "Resize img "
  [^BufferedImage img mode width height]
  (let [{origWidth :width origHeight :height} (dimensions img)
        [targetWidth targetHeight] ((mode fits) origWidth origHeight width height)]
    (when (or (> 3 origWidth)
              (> 3 origHeight))
      (throw+ {:type ::invalid-source-dimensions :width targetWidth :height targetHeight}))
    (when (or (> 3 targetWidth)
              (> 3 targetHeight))
      (throw+ {:type ::invalid-dimensions :width targetWidth :height targetHeight}))
    (.filter (ResampleOp. targetWidth targetHeight) img nil)))


(defn read
  "Reads a BufferedImage from source, something that can be turned into
  a file with clojure.java.io/file"
  [^InputStream source]
  (buffered-image source))

(defn- to-rgb [^BufferedImage i]
  (let [{width :width height :height} (dimensions i)]
    (with-graphics (new-buffered-image width height :rgb)
      (.setColor Color/WHITE)
      (.fillRect 0 0 width height)
      (.drawImage i 0 0 nil))))

(def ^:const jpg-compatible-types #{BufferedImage/TYPE_3BYTE_BGR BufferedImage/TYPE_INT_RGB})

(defn- to-compatible [^BufferedImage img imgformat]
  (let [type (.getType img)]
    (if (and
         (contains? #{:jpg :jpeg} imgformat)
         (not (contains? jpg-compatible-types type)))
      (to-rgb img)
      img)))

(defn write
  "Writes img, a RenderedImage, to dest, an OutputStream
 
  Takes the following keys as options:
 
      :format  - :gif, :jpg, :png or anything supported by ImageIO
      :quality - for JPEG images, a number between 0 and 100"
  [^RenderedImage img ^OutputStream dest & {:keys [format quality] :or {format :jpg}}]
  (assert some? img)
  (assert some? dest)
  (let [compatible-img (to-compatible img format)]
    (if (or (not quality) (not (contains? #{:jpg :jpeg} format)))
      (ImageIO/write ^BufferedImage compatible-img (name format) dest)
      (with-open [imageOut (FileCacheImageOutputStream.  dest nil)] 
        (let [iw (doto ^ImageWriter (first
                                     (iterator-seq
                                      (ImageIO/getImageWritersByFormatName
                                       "jpeg")))
                   (.setOutput imageOut))
              iw-param (doto ^ImageWriteParam (.getDefaultWriteParam iw)
                         (.setCompressionMode ImageWriteParam/MODE_EXPLICIT)
                         (.setCompressionQuality (float (/ quality 100))))
              iio-img (IIOImage. ^BufferedImage compatible-img nil nil)]
          (.write iw nil iio-img iw-param))))))

(def ^:private scalr-methods
  {:auto Scalr$Method/AUTOMATIC
   :balanced Scalr$Method/BALANCED
   :quality Scalr$Method/QUALITY
   :speed Scalr$Method/SPEED
   :ultra-quality Scalr$Method/ULTRA_QUALITY})

(def ^:private scalr-fits
  {:auto Scalr$Mode/AUTOMATIC
   :stretch Scalr$Mode/FIT_EXACT
   :height Scalr$Mode/FIT_TO_HEIGHT
   :width Scalr$Mode/FIT_TO_WIDTH})

(def ^:private scalr-ops
  {:brighten Scalr/OP_BRIGHTER
   :darken Scalr/OP_DARKER
   :grayscale Scalr/OP_GRAYSCALE
   :antialias Scalr/OP_ANTIALIAS})

(defn crop
  "Crops img, a BufferedImage, to the dimensions specificied by x, y, width,
  and height. Returns the cropped image and does not modify img itself."
  [img x y width height]
  (when-not (and (< 0 width)
                 (< 0 height))
    (throw+ {:type ::invalid-crop :dimensions [width height]}))
  (let [{source-width :width source-height :height} (dimensions img)]
    (when (> (+ x width) source-width)
      (throw+ {:type ::invalid-crop-x :x x :crop-width width :source-width source-width}))
    (when (> (+ y height) source-height)
      (throw+ {:type ::invalid-crop-y :y y :crop-height height :source-height source-height})))
  (Scalr/crop img x y width height (make-array BufferedImageOp 0)))

(defn rotate
  [img angle]
  (Scalr/rotate img angle (make-array BufferedImageOp 0)))



(defn ^BufferedImage scale
  "Scales a BufferedImage and returns the result, also a BufferedImage. Leaves
  the original unmodified.
  
  Takes the following options:

      :size     - width/height constraint
      :width    - width constraint  (defaults to :size)
      :height   - height constraint (defaults to :size)
      :method   - Controls the speed/quality tradeoff. Allowed values:
                      :auto, :speed, :balanced, :quality, or :ultra-quality
                  Defaults to :auto.
      :fit      - How an image is fit when the source and destination do not
                  match. Allowed values:
                      :auto, :stretch, :crop, :width, or :height
                  Defaults to :auto. If :crop is given, the image will be
                  cropped with center-anchoring. 
      :ops      - collection of BufferedImageOps to apply to the final image.
                  The follow keywords are also valid:
                      :brighten, :darken, :grayscale, :antialias"
  [^RenderedImage img & {:keys [size width height method fit ops]
                         :or {method :auto fit :auto}}]
  
  (let [width (assert-larger-than-zero (or width size))
        height (assert-larger-than-zero (or height size))
        ops (into-array BufferedImageOp (map #(get scalr-ops % %) ops))
        img-width (.getWidth img)
        img-height (.getHeight img)
        fit* (if (= :crop fit)
               (if (>= (/ height width) (/ img-height img-width))
                 :height
                 :width)
               fit)
        scaled-img (resize img fit* width height)
        ]
    (trace "scaled image with dimenions " img-width "x" img-height " to " (.getWidth scaled-img) "x" (.getHeight scaled-img))
    (if-not (= :crop fit)
      scaled-img
      (let [[x y] (if (= :width fit*)
                    [0 (quot (- (.getHeight scaled-img) height) 2)]
                    [(quot (- (.getWidth scaled-img) width) 2) 0])
            cropped-img (crop scaled-img x y width height)]
        (.flush ^Image scaled-img)
        cropped-img))))

