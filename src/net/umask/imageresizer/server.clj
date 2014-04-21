(ns net.umask.imageresizer.server
  (:require [com.stuartsierra.component :as component]
            [netty.ring.adapter :as netty]))

(defrecord Server [handler]
  component/Lifecycle

  (start [this]
    (assoc this :server (netty/start-server handler {:port 8080})))
  (stop [this]
    ((:server this))))

(defn create-server [handler]
  (->Server handler))
