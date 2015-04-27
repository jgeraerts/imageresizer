(ns net.umask.imageresizer.cache
  (:require [clojure.tools.logging :refer [debug]]))


(defprotocol CacheProtocol
  (store! [this name response])
  (fetch [this name]))

(defn wrap-cache [cache handler]
  (fn [{:keys [uri] :as request}]
    (let [no-cache? (= "no-cache" (get-in request [:headers "cache-control"]))
          cachedresponse (when-not no-cache? (fetch cache uri))]
      (if cachedresponse
        cachedresponse
        (let [response (handler request)]
          (debug "saving response of url" uri request)
          (when (= 200 (:status response))
            (store! cache uri response))
          response)))))

