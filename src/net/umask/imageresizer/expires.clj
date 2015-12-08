(ns net.umask.imageresizer.expires
  (:require [clj-time.core :as t]
            [clj-time.coerce :refer [to-long to-epoch from-long]]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [net.umask.imageresizer.urlparser :refer [option-from-url]]
            [ring.util.response :refer [not-found header]]))


(def  rfc2616formatter (tf/formatter "EEE, dd MMM yyyy HH:mm:ss 'GMT'"))

(def  way-ahead-of-time (to-long (t/date-time 2099 12 31)))

(def ^:const expire-option {:key :expires :re-value #"^([0-9]+)$" :keys [:expires-at]})

(defn wrap-expires [handler]
  (fn [{uri :uri :as request}]
    (let [options (option-from-url uri expire-option)
          expires-at (from-long (get-in options [:expires-at]  way-ahead-of-time))
          now (t/now)]
      (if (t/before? now expires-at)
        (-> (handler (assoc request :uri (:uri options)))
            (header  "expires" (tf/unparse rfc2616formatter expires-at))
            (header  "cache-control" (str "max-age=" (-  (to-epoch expires-at) (to-epoch now)))))
        (not-found "the link has expired")))))
