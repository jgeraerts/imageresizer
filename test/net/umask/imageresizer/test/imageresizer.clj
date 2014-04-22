(ns net.umask.imageresizer.test.imageresizer
  (:use net.umask.imageresizer.resizer
        clojure.test
        ring.mock.request))

(def wrapped-echo (wrap-url-parser identity))

(deftest urlparsing
  (testing "test url parsing"
    (are [x y] (= x (:imageresizer  (wrapped-echo (request :get  y))))
         {:original "filename.jpg"
          :size {:width 300
                 :height 400 }
          :checksum "deadbeef"} "/deadbeef/size/300x400/filename.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}
          :checksum "deadbeef"} "/deadbeef/size/300/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}
          :rotate {:angle  30}
          :checksum "deadbeef"} "/deadbeef/rotate/30/size/300/foo/bar/original.jpg")))
