(ns net.umask.imageresizer.resizer-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.generators :as gen]
            [clojure.tools.logging :refer [debug]]
            [clojure.string :as string]
            [ring.mock.request :refer :all]
            [net.umask.imageresizer.exceptions :refer [wrap-exceptions]]
            [net.umask.imageresizer.graphics :refer [with-graphics]]
            [net.umask.imageresizer.bufferedimage :refer [new-buffered-image dimensions]]
            [net.umask.imageresizer.resizer :refer :all]
            [net.umask.imageresizer.urlparser :refer [wrap-url-parser]]
            [net.umask.imageresizer.memorystore :as memstore]
            [net.umask.imageresizer.cache :refer [CacheProtocol wrap-cache]]
            [net.umask.imageresizer.testutils :refer :all])
  (:import java.io.InputStream
           java.awt.image.BufferedImage
           java.awt.Color
           javax.imageio.ImageIO))

(def ^:const http-ok 200)
(def ^:const white-rgb (.getRGB Color/WHITE))
(def ^:const red-rgb (.getRGB Color/RED))
(def ^:const testimages ["rose.jpg"
                         "portrait.jpg"
                         "landscape.jpg"
                         "rose-cmyk.tiff"
                         "rose-cmyk.jpg"
                         "tux.png"
                         "watermark.png"])

(def not-nil? (complement nil?))

(defn- create-handler [mstore watermarkstore]
  (->> (load-source mstore)       
       (wrap-crop)
       (wrap-scale)
       (wrap-watermark watermarkstore)
       (wrap-output)
       (wrap-url-parser)
       (wrap-exceptions)))

(defn- add-to-store [mstore resource]
  (let [stream (io/input-stream (io/resource resource))]
    (assert (not-nil? stream))
    (do (memstore/memorystore-write mstore resource stream))))

(defn- memory-store []
  (let [mstore (memstore/create-memstore)]
    (doseq [i testimages]
      (add-to-store mstore i))
    mstore))

(defn- getsize [blob]
  (try (let [image (ImageIO/read (io/input-stream blob))]
         [(.getWidth image) (.getHeight image)])
       (catch Throwable e [0 0])))

(defn- test-handler [response image]
  (fn [request]
    {:status response
     :body image}))

(defn- colorfreq [img]
  (let [dim (dimensions img)
        rgbvalues (for [x (range 0 (:width  dim))
                        y (range 0 (:height dim))]
                    (.getRGB img x y))]
    (frequencies rgbvalues)))

(deftest test-scale-wrapper
  (letfn [(run-scale [imgw imgh scaleopts] (:body ((wrap-scale
                                                    (test-handler http-ok
                                                                  (generate-test-image imgw imgh)))
                                                   {:imageresizer {:size scaleopts}})))]
    (testing "testing different sizing options"
      (are [result imgw imgh scaleopts] (= result (dimensions (run-scale imgw imgh scaleopts)))
        {:width 100 :height 50  } 200 100 {:size 100}
        {:width 50  :height 100 } 100 200 {:size 100}
        {:width 100 :height 50  } 200 100 {:width 100}
        {:width 100 :height 200 } 100 200 {:width 100}))
    (testing "check if colors are correct"
      (are [result imgw imgh scaleopts] (= result (colorfreq (run-scale imgw imgh scaleopts)))
        {white-rgb 5000} 200 100 {:size 100}
        {white-rgb 1250 red-rgb 1250} 200 100 {:width 50 :height 50 :color red-rgb}))))

(defn- run-resizer
  "Runs the resizer with a specific uri returns the result as a vector in the form as

  [httpstatus size]
  where
    httpstatus  - the http status returned for the request
    size        - a vector of the resized image in the form of [width height]
    "

  [uri]
  (let [mstore (memory-store)
        handler (create-handler mstore mstore)
        resized (handler (request :get (str "/" uri)))
        status (:status resized)
        size (getsize (:body resized))
        uri-in-mstore (contains? @(:store mstore) uri)]
    [status size]))

(deftest test-resizer
  (testing "testing different sizes/crops"
    (are [result uri] (= result (run-resizer uri))
      [200 [225 300] ] "size/300/portrait.jpg"
      [200 [300 225] ] "size/300/landscape.jpg"
      [200 [200 200] ] "size/200x200/portrait.jpg"
      [200 [200 200] ] "size/200x200/landscape.jpg"
      [200 [100 200] ] "size/100x200/portrait.jpg"
      [200 [100 200] ] "size/100x200/landscape.jpg"
      [200 [100 125] ] "size/100x125/portrait.jpg"
      [200 [100 125] ] "size/100x125/landscape.jpg"
      [200 [125 100] ] "size/125x100/portrait.jpg"
      [200 [125 100] ] "size/125x100/landscape.jpg"
      [200 [100 133] ] "size/100x133/portrait.jpg"
      [200 [100 133] ] "size/100x133/landscape.jpg"
      [200 [133 100] ] "size/133x100/portrait.jpg"
      [200 [133 100] ] "size/133x100/landscape.jpg"
      [200 [200 100] ] "size/200x100/portrait.jpg"
      [200 [200 100] ] "size/200x100/landscape.jpg"
      [200 [200 200] ] "crop/20x20x200x200/portrait.jpg"
      [200 [200 200] ] "crop/20x20x200x200/landscape.jpg"
      [200 [100 100] ] "size/100/crop/20x20x200x200/portrait.jpg"
      [200 [100 100] ] "size/100/crop/20x20x200x200/landscape.jpg"
      [200 [200 267] ] "size/200w/portrait.jpg"
      [200 [200 150] ] "size/200w/landscape.jpg"
      [200 [150 200] ] "size/200h/portrait.jpg"
      [200 [267 200] ] "size/200h/landscape.jpg"
      [200 [200 200] ] "size/200x200-0xDDDDDD/portrait.jpg"
      [200 [200 200] ] "size/200x200-0xDDDDDD/landscape.jpg"
      [200 [200 267] ] "watermark/10x10-watermark.png/size/200w/portrait.jpg"))
  (testing "testing different input formats"
    (are [result uri] (= result (run-resizer uri))
      [200 [200 200] ] "size/200x200/rose.jpg"
      [200 [200 200] ] "size/200x200/rose-cmyk.jpg"
      [200 [200 200] ] "size/200x200/rose-cmyk.tiff"
      [200 [200 200] ] "size/200x200/tux.png")))

(deftest test-non-existing-image
  (let [secret "secret"
        mstore (memory-store)
        handler (create-handler mstore mstore)]
    (testing "a non existing original should return 404"
      (let [resized (handler (request :get "/size/200x200/nonexisting"))]
        (is (= 404 (:status resized)))))))

(deftest test-nil-store
  (let [ handler (create-handler nil nil)]
    (let [result (handler (request :get "/size/200x400/foo"))]
      (is (= 404 (:status result))))))

(deftest test-nil-wmarkstore
  (let [mstore (memory-store)
        handler (create-handler mstore nil)
        result (handler (request :get "/watermark/topleft-watermark.png/size/200x100/rose.jpg"))]
    (is (= 200 (:status result)))))

(defspec test-handler-should-return-404
  100
  (let [mstore (memory-store)
        handler (create-handler mstore mstore)]
    (prop/for-all [v (gen/fmap clojure.string/join
                               (gen/vector
                                (gen/one-of [gen/char-alphanumeric (gen/return \/)])))]
                  (= 404 (:status (handler {:uri v
                                            :request-method :get}))))))

(def ^:const valid-responses #{200 400})

(def gen-color (gen/large-integer* {:max 0xFFFFFF :min 0}))
(def gen-size (gen/large-integer* {:max 3000 :min 0}))
(def gen-width-size (gen/fmap #(str % "w") gen-size))
(def gen-height-size (gen/fmap #(str % "h") gen-size))
(def gen-width-times-height-size (gen/fmap #(apply format "%dx%d" %) (gen/tuple gen-size gen-size)))
(def gen-width-times-height-color-size (gen/fmap #(apply format "%dx%d-0x%06X" %) (gen/tuple gen-size gen-size gen-color)))

(def ^:const watermark-anchors ["topleft"
                                "topcenter"
                                "topright"
                                "midleft"
                                "midcenter"
                                "midright"
                                "bottomright"
                                "bottomcenter"
                                "bottomright"])

(def gen-watermark
  (gen/one-of [(gen/return [])
               (gen/tuple (gen/return "watermark")
                          (gen/fmap #(apply format "%s-%s" %)
                                    (gen/tuple
                                     (gen/one-of
                                      [(gen/elements watermark-anchors)
                                                  (gen/fmap #(apply format "%dx%d" %)
                                                            (gen/tuple gen-size gen-size))])
                                     (gen/elements ["watermark.png"]))))]))

(def gen-crop
  (gen/one-of [(gen/return [])
               (gen/tuple (gen/return "crop")
                          (gen/fmap #(apply format "%dx%dx%dx%d" %) (gen/vector gen-size 4)))]))

(def gen-sizes
  (gen/one-of [(gen/return [])
               (gen/tuple (gen/return "size")
                          (gen/one-of [gen-size
                                       gen-width-size
                                       gen-height-size
                                       gen-width-times-height-size
                                       gen-width-times-height-color-size]))]))

(def gen-output-format
  (gen/one-of [(gen/return [])
               (gen/tuple (gen/return "output")
                          (gen/one-of [(gen/return "png")
                                       (gen/return "jpg")]))]))

(defspec test-resizing
  100
  (let [mstore (memory-store)
        handler (create-handler mstore mstore)]
    (prop/for-all [o (gen/elements testimages)
                   watermark gen-watermark
                   output gen-output-format
                   crop gen-crop
                   size gen-sizes]
                  (let [url (string/join \/ (conj (into [] (concat output watermark size crop)) o))
                        response (handler (request :get (str "/" url)))]
                    (debug "url  " url " gave response" response)
                    (contains? valid-responses (:status response))))))
