(ns net.umask.imageresizer.server
  (:require [com.stuartsierra.component :as component]
            [netty.ring.adapter :as netty]))

(defn- vhost-handler [vhosts]
  (fn [request]
    (let [hostname (:server-name request)
          handler (get-in vhosts [hostname :handler])]
      (if-not (nil? handler)
        (handler request)
        {:status 404
         :body "vhost config not found"}))))

(defrecord Server [nettyconfig vhost]
  component/Lifecycle

  (start [this]
    (print this)
    (assoc this :server (netty/start-server (vhost-handler (:vhost this)) (:nettyconfig this))))
  (stop [this]
    ((:server this))))

(defn create-server
  ([]
     (map->Server {:nettyconfig {:port 8080}}))
  ([nettyconfig]
     (map->Server {:nettyconfig nettyconfig})))
