(ns net.umask.imageresizer.test.filestore
   (:use net.umask.imageresizer.filestore
          net.umask.imageresizer.store
          clojure.test)
   (:require [clojure.java.io :as io]
             ))

(deftest savefile
  (let [tempdir (java.io.File/createTempFile "filestorebasedir" "")
        fstore (->FileStore tempdir)
        testdata "testdata"]
    (doto tempdir (.delete) (.mkdir))
    (.mkdirs tempdir)
    (store-write fstore "mekker" (io/input-stream (.getBytes testdata "UTF8")))
    (is (.exists (io/file tempdir "mekker")))
    (is (= testdata (slurp (io/file tempdir "mekker"))))))

