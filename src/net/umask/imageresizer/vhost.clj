(ns net.umask.imageresizer.vhost)

(defn vhost-handler [vhosts]
  (fn [request]
    (let [hostname (:server-name request)
          handler (get-in vhosts [hostname :handler])]
      (if-not (nil? handler)
        (handler request)
        {:status 404
         :body "vhost config not found"}))))
