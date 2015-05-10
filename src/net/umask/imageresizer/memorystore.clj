(ns net.umask.imageresizer.memorystore
  (:require [clojure.java.io :as io]
            [net.umask.imageresizer.store :refer :all])
  (:import (java.io ByteArrayOutputStream)))

(defn- tobytes [stream]
  (let [bos (ByteArrayOutputStream.)]
    (do
      (io/copy stream bos))
    (.toByteArray bos)))

(defrecord MemoryStore [store]
  Store
  (store-write [this name stream]
    (swap! (:store this) assoc name (tobytes stream))
    )
  (store-read [this name]
    (if-let [b (get @(:store this) name)]
      (io/input-stream b))))

(defn create-memstore []
  (->MemoryStore (atom {})))
