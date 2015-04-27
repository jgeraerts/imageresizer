(ns net.umask.imageresizer.bytebuffer
  (:import java.nio.ByteBuffer))

(def ^:const array-of-bytes-type (Class/forName "[B")) 

(defn- bytes? [i]
  (= (type i) array-of-bytes-type))

(defn wrap-bytebuffer [handler]
  (fn [request]
    (let [response (handler request)
          body (:body response)]
      (if (bytes? body)
        (assoc response :body (ByteBuffer/wrap body))
        response))))
