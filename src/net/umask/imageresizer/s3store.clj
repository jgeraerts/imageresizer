(ns net.umask.imageresizer.s3store
  (:require [aws.sdk.s3 :as s3]
            [clojure.tools.logging :refer [debug]]
            [clojure.java.io :as io]
            [clojure.string :refer [lower-case]]
            [net.umask.imageresizer.cache :as c]
            [net.umask.imageresizer.source :as source]
            [net.umask.imageresizer.util :refer [trim-leading-slash]]
            [ring.util.response :as response])
  (:import (com.amazonaws AmazonServiceException)
           (java.io IOException)))

(defn- write-to-tempfile [in]
  (let [tempfile (java.io.File/createTempFile "s3store" "")]
    (try 
      (io/copy in tempfile)
      (catch IOException e
        (.delete tempfile)
        (throw (ex-info "cannot write to temporary file"  e))))
    tempfile))

(defn- get-object [this name]
  (try
    (s3/get-object (:cred this) (:bucket this) name)
    (catch AmazonServiceException e
      (when-not (= 404 (.getStatusCode e))
        (throw e)))))

(defrecord S3ImageSource [bucket cred]
  source/ImageSource
  (get-image-stream [this name]
    (:content (get-object this name))))

(defrecord S3Cache [bucket cred public]
  c/CacheProtocol
  (store! [this name response]
    (let [response-headers (:headers response)
          s3-headers {:content-type (get response-headers "Content-Type")}
          tempfile (write-to-tempfile (:body response))
          grants (when public (s3/grant :all-users :read))]
      (try 
        (s3/put-object (:cred this) (:bucket this)
                       (trim-leading-slash name) tempfile
                       s3-headers
                       grants)
        (finally (.delete tempfile)))))
  (fetch [this name]
    (when-let [s3response (get-object this (trim-leading-slash name))]
      (-> (response/response (:content s3response))
          (response/content-type (:content-type s3response))))))
  

(defn create-s3source [bucket cred]
  (->S3ImageSource bucket cred))

(defn create-s3cache [bucket cred]
  (->S3Cache bucket cred true))
