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
   [net.umask.imageresizer.resizer :as resizer]
   [net.umask.imageresizer.memorystore :as memstore]
   [net.umask.imageresizer.server :as imgserver]
   [net.umask.imageresizer.store :as store]
   [net.umask.imageresizer.s3store :as s3store])
  (:use ring.middleware.params))


  
(def secret "verysecret")

(defn buildurl [url]
  (let [checksum (digest/md5 (str  secret url))]
    (str "/" checksum "/" url)))

(defn create-test-system []
  (let [mstore (memstore/create-memstore)
        rose (io/input-stream (io/resource "rose.jpg"))]
    (do
      (store/store-write mstore "rose.jpg" rose))
    (component/system-map
     :vhost {"localhost" (resizer/create-resizer secret mstore)}
     :server  (component/using  (imgserver/create-server) [:vhost]))))


(reloaded.repl/set-init! create-test-system)

