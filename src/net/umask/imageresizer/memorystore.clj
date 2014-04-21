(ns net.umask.imageresizer.memorystore
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io])
  (:use net.umask.imageresizer.store)
  (:import  java.io.ByteArrayOutputStream))

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
