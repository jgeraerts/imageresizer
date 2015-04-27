(ns net.umask.imageresizer.checksum
  (:require [clojure.tools.logging :refer [warn]]
            [ring.util.response :as response]))

(defn- extract-checksum [^String uri]
  (let [checksum-start-idx (inc (.indexOf uri "/"))
        checksum-end-idx (.indexOf uri "/" checksum-start-idx)]
    (when (and (= 1 checksum-start-idx)
             (> checksum-end-idx checksum-start-idx))
      [(subs uri checksum-start-idx checksum-end-idx)
       (subs uri checksum-end-idx)])))

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
  (fn [request]
    (let [uri (:uri request)
          [checksum leftover] (extract-checksum uri)
          checksumvalid? (validate-checksum checksum secret leftover)]
      (if checksumvalid?
        (handler (assoc request :uri leftover :origuri uri))
        (invalid-checksum request)))))
