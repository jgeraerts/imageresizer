(ns net.umask.imageresizer.server
  (:require [com.stuartsierra.component :as component]
            [netty.ring.adapter :as netty]))

(defrecord Server [resizer]
  component/Lifecycle

  (start [this]
    (print this)
    (assoc this :server (netty/start-server (:handler (:resizer this)) {:port 8080})))
  (stop [this]
    ((:server this))))

(defn create-server []
  (map->Server {}))
