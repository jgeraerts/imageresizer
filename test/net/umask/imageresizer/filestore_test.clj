(ns net.umask.imageresizer.filestore_test
   (:use net.umask.imageresizer.filestore
          net.umask.imageresizer.store
          clojure.test)
   (:require [clojure.java.io :as io]
             ))

(deftest filestore
  (let [tempdir (java.io.File/createTempFile "filestorebasedir" "")
        fstore (->FileStore tempdir)
        testdata "testdata"
        filename "foo/bar/mekker"]
    (doto tempdir (.delete) (.mkdir))
    (.mkdirs tempdir)
    (store-write fstore filename (io/input-stream (.getBytes testdata "UTF8")))
    (is (.exists (io/file tempdir filename)))
    (is (= testdata (slurp (io/file tempdir filename))))))

