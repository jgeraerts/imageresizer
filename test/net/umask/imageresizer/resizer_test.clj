(ns net.umask.imageresizer.resizer-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.mock.request :refer :all]
            [net.umask.imageresizer.resizer :refer :all]
            [net.umask.imageresizer.urlparser :refer [wrap-url-parser]]
            [net.umask.imageresizer.memorystore :as memstore]
            [net.umask.imageresizer.store :as store]
            [net.umask.imageresizer.cache :refer [CacheProtocol wrap-cache]])
  (:import java.io.InputStream
           java.awt.image.BufferedImage
           javax.imageio.ImageIO))

(defn- create-handler [mstore]
  (->> (create-ring-handler mstore)
       (wrap-url-parser)))

(defn- add-to-store [mstore resource]
  (let [stream (io/input-stream (io/resource resource))]
    (do (memstore/memorystore-write mstore resource stream))))

(defn- memory-store []
  (let [mstore (memstore/create-memstore)]
    (doseq [i ["rose.jpg" "portrait.jpg" "landscape.jpg" "rose-cmyk.tiff" "rose-cmyk.jpg" "tux.png"]]
      (add-to-store mstore i))
    mstore))

(defn- getsize [blob]
  (try (let [image (ImageIO/read (io/input-stream blob))]
         [(.getWidth image) (.getHeight image)])
       (catch Throwable e [0 0])))

(defn- run-resizer
  "Runs the resizer with a specific uri returns the result as a vector in the form as

  [httpstatus size]
  where
    httpstatus  - the http status returned for the request
    size        - a vector of the resized image in the form of [width height]
    "

  [uri]
  (let [mstore (memory-store)
        handler (create-handler mstore )
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
      [200 [100 100] ] "crop/20x20x200x200/size/100/portrait.jpg"
      [200 [100 100] ] "crop/20x20x200x200/size/100/landscape.jpg"
      [200 [200 267] ] "size/200w/portrait.jpg"
      [200 [200 150] ] "size/200w/landscape.jpg"
      [200 [150 200] ] "size/200h/portrait.jpg"
      [200 [267 200] ] "size/200h/landscape.jpg"
      [200 [200 200] ] "size/200x200-0xDDDDDD/portrait.jpg"
      [200 [200 200] ] "size/200x200-0xDDDDDD/landscape.jpg"))
  (testing "testing different input formats"
    (are [result uri] (= result (run-resizer uri))
      [200 [200 200] ] "size/200x200/rose.jpg"
      [200 [200 200] ] "size/200x200/rose-cmyk.jpg"
      [200 [200 200] ] "size/200x200/rose-cmyk.tiff"
      [200 [200 200] ] "size/200x200/tux.png")))

(deftest test-non-existing-image
  (let [secret "secret"
        mstore (memory-store)
        handler (create-handler mstore)]
    (testing "a non existing original should return 404"
      (let [resized (handler (request :get "/size/200x200/nonexisting"))]
        (is (= 404 (:status resized)))))))
