(ns net.umask.imageresizer.exceptions
  (:require [clojure.tools.logging :refer [error]]
            [ring.util.response :as response]
            [slingshot.slingshot :refer [try+]]))


(defn bad-request [message]
  (-> (response/response message)
      (response/content-type "text/plain")
      (response/status 400)))

(defn wrap-exceptions [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :net.umask.imageresizer.image/invalid-dimensions] {:keys [:width :height]}
       (let [msg (str "cannot resize to dimensions " width "x" height
                      ". The dimensions should be minimal 3x3")] 
         (error msg)
         (bad-request msg)))
     (catch Object _
       (error (:throwable &throw-context) "unexpected exception when rendering request " request)
       (-> (response/response "server error")
           (response/content-type "text/plain")
           (response/status 500))))))
