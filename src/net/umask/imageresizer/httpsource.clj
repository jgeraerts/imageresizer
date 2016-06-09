(ns net.umask.imageresizer.httpsource
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]
            [clojure.tools.logging :refer [debug]]
            [net.umask.imageresizer.source :refer [ImageSource]]))

(defn- build-url
  [url name  {:keys [fromregex replacement] :as rewrite}]
  
  (let [rewritten-name (if (empty? rewrite)
                         name
                         (string/replace name fromregex replacement))]
    (str url rewritten-name)))

(defrecord HttpImageSource [url conn-timeout sock-timeout rewrite]
  ImageSource
  (get-image-stream [this name]
    (let [source-url (build-url url name rewrite)
          {:keys [body error] :as resp} @(http/get source-url)]
      (debug "response for url " source-url " : " resp)
      (when-not error body))))


(defn create-httpsource
  [url & {:keys [conn-timeout sock-timeout rewrite] :or {conn-timeout 1000
                                                         sock-timeout 1000
                                                         rewrite {}}}]
  (->HttpImageSource
   url conn-timeout sock-timeout rewrite))
