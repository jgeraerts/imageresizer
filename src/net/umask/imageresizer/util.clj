(ns net.umask.imageresizer.util
  (:require [clojure.string :as string]
            [slingshot.slingshot :refer [throw+]]))

(defn trim-leading-slash [str]
  (let [firstchar (first (seq str))]
    (if (= \/ firstchar)
      (subs str 1)
      str)))

(defn split-uri-path [uri limit]
  (string/split (trim-leading-slash uri) #"/" limit))

(defn assert-larger-than-zero [x]
  (if (> x 0)
    x
    (throw+ {:type :net.umask.imageresizer.image/invalid-dimensions})))
