(ns net.umask.imageresizer.resizer-test
  (:use net.umask.imageresizer.resizer
        clojure.test
        ring.mock.request)
  (:require [net.umask.imageresizer.memorystore :as memstore]
            [net.umask.imageresizer.store :as store]
            [clojure.java.io :as io]
            digest)
  (:import java.io.InputStream
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

(defn- add-to-store [mstore resource]
  (let [stream (io/input-stream (io/resource resource))]
    (do (store/store-write mstore resource stream))))

(defn- memory-store []
  (let [mstore (memstore/create-memstore)]
    (doseq [i ["rose.jpg" "portrait.jpg" "landscape.jpg" "rose-cmyk.tiff" "rose-cmyk.jpg"]]
      (add-to-store mstore i))
    mstore))

(defn- addchecksum [secret url]
  (str "/" (digest/md5 (str secret url)) "/" url))

(defn- getsize [^InputStream stream]
  (try (let [image (ImageIO/read stream)]
         [(.getWidth image) (.getHeight image)])
       (catch Throwable e [0 0])
       (finally (.close stream))))

(deftest test-non-existing-image
  (let [secret "secret"
        mstore (memory-store)
        resizer (create-resizer secret mstore)
        handler (:handler resizer)]
        (testing "a non existing original should return 404"
      (let [resized (handler (request :get (addchecksum secret  "size/200x200/nonexisting")))]
        (is (= 404 (:status resized)))))))

(defn- run-resizer
  "Runs the resizer with a specific uri returns the result as a vector in the form as

  [httpstatus size instore]
  where
    httpstatus  - the http status returned for the request
    size        - a vector of the resized image in the form of [width height]
    instore     - a boolean indicating the image uri was found in the store after resizing"

  [uri]
  (let [secret "secret"
        mstore (memory-store)
        resizer (create-resizer secret mstore)
        handler (:handler resizer)
        resized (handler (request :get (addchecksum secret uri)))
        status (:status resized)
        size (getsize (:body resized))
        uri-in-mstore (contains? @(:store mstore) uri)]
    [status size uri-in-mstore]))

(deftest test-resizer
  (are [result uri] (= result (run-resizer uri))
       [200 [225 300] true] "size/300/portrait.jpg"
       [200 [300 225] true] "size/300/landscape.jpg"
       [200 [200 200] true] "size/200x200/portrait.jpg"
       [200 [200 200] true] "size/200x200/landscape.jpg"
       [200 [100 200] true] "size/100x200/portrait.jpg"
       [200 [100 200] true] "size/100x200/landscape.jpg"
       [200 [100 125] true] "size/100x125/portrait.jpg" 
       [200 [100 125] true] "size/100x125/landscape.jpg"
       [200 [125 100] true] "size/125x100/portrait.jpg" 
       [200 [125 100] true] "size/125x100/landscape.jpg"
       [200 [100 133] true] "size/100x133/portrait.jpg" 
       [200 [100 133] true] "size/100x133/landscape.jpg"
       [200 [133 100] true] "size/133x100/portrait.jpg" 
       [200 [133 100] true] "size/133x100/landscape.jpg"
       [200 [200 100] true] "size/200x100/portrait.jpg"
       [200 [200 100] true] "size/200x100/landscape.jpg"
       [200 [200 200] true] "crop/20x20x200x200/portrait.jpg"
       [200 [200 200] true] "crop/20x20x200x200/landscape.jpg"
       [200 [100 100] true] "crop/20x20x200x200/size/100/portrait.jpg"
       [200 [100 100] true] "crop/20x20x200x200/size/100/landscape.jpg"
       [200 [200 267] true] "size/200w/portrait.jpg"
       [200 [200 150] true] "size/200w/landscape.jpg"
       [200 [150 200] true] "size/200h/portrait.jpg"
       [200 [267 200] true] "size/200h/landscape.jpg"
       [200 [200 200] true] "size/200x200-0xDDDDDD/portrait.jpg"
       [200 [200 200] true] "size/200x200-0xDDDDDD/landscape.jpg"
       [200 [200 200] true] "size/200x200/rose.jpg"
       [200 [200 200] true] "size/200x200/rose-cmyk.jpg"
       [200 [200 200] true] "size/200x200/rose-cmyk.tiff"))
