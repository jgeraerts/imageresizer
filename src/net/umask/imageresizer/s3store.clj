(ns net.umask.imageresizer.s3store
  (:require [aws.sdk.s3 :as s3])
  (:use net.umask.imageresizer.store))

(defrecord S3Store [bucket cred]
  Store
  (store-write [this name stream]
    (s3/put-object (:cred this) (:bucket this) name stream))
  (store-read [this name]
    (s3/get-object (:cred this) (:bucket this) name)))

(defn create-s3store [bucket cred]
  (->S3Store bucket cred))
