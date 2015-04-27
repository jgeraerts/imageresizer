(ns net.umask.imageresizer.nio
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io])
  (:import (java.nio.file FileSystems Files LinkOption OpenOption Path)
           (java.nio.file.attribute FileAttribute)))

(defn get-path
  "translates the given string to a java.nio.file.Path object"
  [^String name]
  (.. (FileSystems/getDefault)
      (getPath name (make-array String 0))))

(defn resolve
  ([^Path parent other]
   (.resolve parent other))
  ([^Path parent other & more]
   (reduce resolve (resolve parent other) more)))

(defn parent
  [^Path path]
  (.getParent path))

(defn exists
  [^Path path]
  (Files/exists path (make-array LinkOption 0)))

(defn createDirectories
  [^Path path]
  (Files/createDirectories path (make-array FileAttribute 0)))

(defn createTempDirectory [prefix]
  (Files/createTempDirectory prefix (make-array FileAttribute 0)))

(extend Path
  io/IOFactory
  (assoc io/default-streams-impl
         :make-input-stream (fn [x opts] (Files/newInputStream x (make-array OpenOption 0)))
         :make-output-stream (fn [x opts] (Files/newOutputStream x (make-array OpenOption 0)))))
