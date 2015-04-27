(ns net.umask.imageresizer.filestore_test
  (:require [net.umask.imageresizer.filestore :refer :all]
            [net.umask.imageresizer.cache :as c]
            [net.umask.imageresizer.source :refer [get-image-stream]]
            [ring.util.response :as response]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [java.nio.file Files attribute.FileAttribute]
           org.apache.commons.io.FileUtils
           java.io.ByteArrayOutputStream))

(def ^:private testblob (.getBytes "once upton a time"))

(def ^:private testresponse (-> (response/response testblob)
                                (response/header "Content-Length" (alength testblob))))

(def ^:dynamic basedir)

(defn temporary-dir-fixture [f]
  (let [directory (Files/createTempDirectory "filestoretest" (into-array FileAttribute []))]
    (binding [basedir directory]
      (f))
    (FileUtils/deleteDirectory (.toFile directory))))

(use-fixtures :each temporary-dir-fixture)

(deftest filestore-cache
  (let [name "/cachedresource"
        cache (create-filecache (.toString  basedir))]
    (testing "cache should retrieve same data as stored"
      (c/store! cache name testresponse)
      (let [retrievedfromcache (c/fetch cache name)
            bos (ByteArrayOutputStream.)]
        (when-not (nil? retrievedfromcache) (io/copy (:body retrievedfromcache) bos))
        (is (= (seq testblob) (seq (.toByteArray bos))))
        (is (= (dissoc retrievedfromcache :body) (dissoc testresponse :body)))))))

(deftest filesource
  (let [source (create-filesource (.toString basedir))]
    (is (nil? (get-image-stream source "/does-not-exist"))))
  )
