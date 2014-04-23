(ns net.umask.imageresizer.s3store
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io])
  (:use net.umask.imageresizer.store)
  (:import com.amazonaws.AmazonServiceException))

(defrecord S3Store [bucket cred]
  Store
  (store-write [this name stream]
    (let [tempfile (java.io.File/createTempFile "s3store" "")]
      (try
        (do
          (io/copy stream tempfile)
          (s3/put-object (:cred this) (:bucket this) name tempfile))
        (finally (.delete tempfile)))))
  (store-read [this name]
    (try 
      (:content (s3/get-object (:cred this) (:bucket this) name))
      (catch AmazonServiceException e
        (if (= 404 (.getStatusCode e))
          nil
          (throw e))))))

(defn create-s3store [bucket cred]
  (->S3Store bucket cred))
