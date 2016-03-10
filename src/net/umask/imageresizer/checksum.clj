(ns net.umask.imageresizer.checksum
  (:require [clojure.tools.logging :refer [warn]]
            digest
            [ring.util.response :as response]
            [net.umask.imageresizer.util :refer [split-uri-path]]))

(defn- validate-checksum [checksum secret uri]
  (when uri
    (let [calculated-checksum (digest/md5 (str secret (subs uri 1)))]
      (= checksum calculated-checksum))))

(defn- invalid-checksum
  [request]
  (warn (str "invalid checksum for uri " (:uri request)))
  (-> (response/response "invalid checksum")
      (response/status 400)
      (response/content-type "text/plain")))

(defn wrap-checksum [secret handler]
  (fn [{uri :uri :as request}]
    (let [[checksum leftover] (split-uri-path uri 2)
          new-uri (str "/" leftover)]
      (if (validate-checksum checksum secret new-uri)
        (handler (assoc request :uri new-uri :origuri uri))
        (invalid-checksum request)))))
