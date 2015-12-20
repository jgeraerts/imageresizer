(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [reloaded.repl :refer [system init start stop go reset]]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [com.stuartsierra.component :as component]
   [aws.sdk.s3 :as s3]
   [digest]
   [ring.middleware.params :refer :all]
   [net.umask.imageresizer.resizer :as resizer]
   [net.umask.imageresizer.memorystore :as memstore]
   [net.umask.imageresizer.filestore :refer [create-filecache]]
   [net.umask.imageresizer.server :as imgserver]
   [net.umask.imageresizer.s3store :as s3store]
   [net.umask.imageresizer.bufferedimage :refer [buffered-image]]
   [net.umask.imageresizer.watermark :refer [watermark]]
   [net.umask.imageresizer.image :as img])
  (:import
   [javax.imageio ImageIO]
   [java.awt.image BufferedImage]
   [java.awt Graphics]
   [javax.swing JFrame JPanel JLabel ImageIcon]))


(defn- image-panel [image]
  (doto (JPanel.)
    (.add (JLabel. (ImageIcon. (buffered-image image))))))

(defn display-image [image]
  (doto (JFrame.)
    (.add ^java.awt.Component (image-panel image))
    (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
    (.setVisible true)
    (.pack)
    (.show)))


(def secret "verysecret")

(defn buildurl [url]
  (let [checksum (digest/md5 (str  secret url))]
    (str "/" checksum "/" url)))

(defn display-url [uri]
  (let [url (str "http://localhost:8080" (buildurl uri))]
    (println (str "displaying " url))
    (display-image (java.net.URL. url))))

(defn create-test-system []
  (let [mstore (memstore/create-memstore)
        cache (create-filecache "/temp/ilecache")
        rose (io/input-stream (io/resource "rose.jpg"))
        watermark (io/input-stream (io/resource "watermark.png"))]
    (memstore/memorystore-write  mstore "rose.jpg" rose)
    (memstore/memorystore-write  mstore "watermark.png" watermark)
    (component/system-map
     :vhost {"localhost" (resizer/create-resizer secret mstore mstore cache)}
     :server  (component/using  (imgserver/create-server) [:vhost]))))


(reloaded.repl/set-init! create-test-system)

