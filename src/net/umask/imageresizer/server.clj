(ns net.umask.imageresizer.server
  (:require [com.stuartsierra.component :as component]
            [net.umask.imageresizer.vhost :refer [vhost-handler]]
            [org.httpkit.server :as httpkit]))



(defrecord Server [httpconfig vhost]
  component/Lifecycle

  (start [this]
    (assoc this :server (httpkit/run-server (vhost-handler (:vhost this)) (:httpconfig this))))
  (stop [this]
    (when (:server this) 
      ((:server this)))))

(defn create-server
  ([]
     (map->Server {:httpconfig {:port 8080}}))
  ([httpconfig]
     (map->Server {:httpconfig httpconfig})))
