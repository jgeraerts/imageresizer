(ns net.umask.imageresizer.resizer_test
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


(defn- memory-store []
  (let [mstore (memstore/create-memstore)
        rose (io/input-stream (io/resource "rose.jpg"))]
    (do (store/store-write mstore "rose.jpg" rose)
        mstore)))

(defn- addchecksum [secret url]
  (str "/" (digest/md5 (str secret url)) "/" url))

(defn- getsize [^InputStream stream]
  (try (let [image (ImageIO/read stream)]
         [(.getWidth image) (.getHeight image)])
       (catch Throwable e [0 0])
       (finally (.close stream))))

(deftest test-scaling
  (let [secret "secret"
        mstore (memory-store)
        resizer (create-resizer secret mstore)
        handler (:handler resizer)]
    (testing "resize a rose"
      (let [uri (addchecksum secret "size/300/rose.jpg")
            resized  (handler (request :get uri))]
        (is (= [300 242] (getsize (:body  resized))))
        (is (contains? @(:store mstore) (subs  uri 1))))
      (let [uri (addchecksum secret "size/200x200/rose.jpg")
            resized  (handler (request :get uri))]
        (is (= [200 200] (getsize (:body  resized))))
        (is (contains? @(:store mstore) (subs  uri 1))))
      (let [uri (addchecksum secret "crop/20x20x200x200/rose.jpg")
            resized  (handler (request :get uri))]
        (is (= [200 200] (getsize (:body  resized))))
        (is (contains? @(:store mstore) (subs  uri 1))))
      (let [uri (addchecksum secret "rotate/30/size/200x200/rose.jpg")
            resized  (handler (request :get uri))]
        (is (= [200 200] (getsize (:body  resized))))
        (is (contains? @(:store mstore) (subs  uri 1)))))))
