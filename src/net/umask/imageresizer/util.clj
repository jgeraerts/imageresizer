(ns net.umask.imageresizer.util
  (:require [clojure.string :as string]))

(defn trim-leading-slash [str]
  (let [firstchar (first (seq str))]
    (if (= \/ firstchar)
      (subs str 1)
      str)))

(defn split-uri-path [uri limit]
  (string/split (trim-leading-slash uri) #"/" limit))
