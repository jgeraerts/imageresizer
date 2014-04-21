(ns net.umask.imageresizer.memorystore
  (:require [com.stuartsierra.component :as component])
  (:use net.umask.imageresizer.store))

(defrecord MemoryStore []
  component/Lifecycle
  (start [this]
    (assoc this :store (atom {})))
  (stop [this])
  Store
  (store-write [this name stream]
    (swap! (:store this) assoc name stream)
    )
  (store-read [this name]
    (get @(:store this)) name))

(defn create-memstore []
  (->MemoryStore))
