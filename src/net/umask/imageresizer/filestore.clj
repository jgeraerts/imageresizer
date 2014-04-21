(ns net.umask.imageresizer.filestore
  (:require [clojure.java.io :as io])
  (:use net.umask.imageresizer.store))

(defn getfile [ basedir name] 
    (io/file basedir name))

(defrecord FileStore [basedir]
  Store
  (store-write [this name stream]
    (io/copy stream (getfile (:basedir this) name)))

  (store-read [this name]
    (io/input-stream (getfile (:basedir this) name))))

(defn file-store [basedir]
  (->FileStore basedir))
