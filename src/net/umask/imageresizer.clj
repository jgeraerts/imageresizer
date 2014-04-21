(ns net.umask.imageresizer
  (:require
   [netty.ring.adapter :as netty]))


(defn handler [req]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello world"})


