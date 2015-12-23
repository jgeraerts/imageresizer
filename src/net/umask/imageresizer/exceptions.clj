(ns net.umask.imageresizer.exceptions
  (:require [clojure.tools.logging :refer [error]]
            [ring.util.response :as response]
            [slingshot.slingshot :refer [try+]]))


(defn bad-request [message]
  (error message)
  (-> (response/response message)
      (response/content-type "text/plain")
      (response/status 400)))

(defn wrap-exceptions [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :net.umask.imageresizer.image/invalid-dimensions] {:keys [:width :height]}
       (bad-request
        (format "Invalid resize dimensions. They should be minimal 3x3 but is %dx%d"
                width height)))
     (catch [:type :net.umask.imageresizer.image/invalid-source-dimensions] {:keys [:width :height]}
       (bad-request
        (format "Invalid source image dimensions. The source should be minimal 3x3 but is [%dx%d]"
                width height)))
     (catch [:type :net.umask.imageresizer.image/invalid-crop-x] {:keys [x crop-width source-width]} 
       (bad-request
        (format "Invalid crop bounds. x + cropwidth [%d + %d] should be <= source width [%d]"
                x crop-width source-width)))
     (catch [:type :net.umask.imageresizer.image/invalid-crop-y] {:keys [y crop-height source-height]} 
       (bad-request
        (format "Invalid crop bounds. y + cropheight [%d + %d] should be <= source height [%d]"
                y crop-height source-height)))
     (catch [:type :net.umask.imageresizer.image/invalid-crop] {:keys [:dimensions]}
       (bad-request
        (apply format
               "Invalid crop arguments. Width and height should be larger than 0 but are %dx%d"
               dimensions)))
     (catch Object _
       (error (:throwable &throw-context) "unexpected exception when rendering request " request)
       (-> (response/response "server error")
           (response/content-type "text/plain")
           (response/status 500))))))
