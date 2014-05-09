(ns net.umask.imageresizer.server
  (:use [com.net.umask.imageresizer :only [vhost-handler]])
  (:require [com.stuartsierra.component :as component]
            [netty.ring.adapter :as netty]))



(defrecord Server [nettyconfig vhost]
  component/Lifecycle

  (start [this]
    (assoc this :server (netty/start-server (vhost-handler (:vhost this)) (:nettyconfig this))))
  (stop [this]
    ((:server this))))

(defn create-server
  ([]
     (map->Server {:nettyconfig {:port 8080}}))
  ([nettyconfig]
     (map->Server {:nettyconfig nettyconfig}))  )
