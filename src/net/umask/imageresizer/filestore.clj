(ns net.umask.imageresizer.filestore
  (:require [clojure.java.io :as io])
  (:use net.umask.imageresizer.store))

(defn getfile [ basedir name] 
    (io/file basedir name))

(defrecord FileStore [basedir]
  Store
  (store-write [this name stream]
    (let [f (getfile (:basedir this) name)
          parent (.getParentFile f)
          parent-exists? (.exists parent)]
      (do  (if (not parent-exists?) (.mkdirs parent))
           (io/copy stream (getfile (:basedir this) name)))))

  (store-read [this name]
    (io/input-stream (getfile (:basedir this) name))))

(defn create-filestore [basedir]
  (->FileStore basedir))
