(ns net.umask.imageresizer.urlparser_test
  (:use net.umask.imageresizer.urlparser
        clojure.test
        ring.mock.request))

(def ^:private secret "verysecret")

(def ^:private handler (wrap-url-parser secret identity) )

(defn- calculate-checksum [url]
  (digest/md5 (str secret url)))

(defn- call-handler-with-correct-checksum [url]
  (let [checksum (calculate-checksum url)]
    (handler (request :get (str "/" checksum "/" url)))))

(deftest urlparsing
  (testing "check if imageresizer parameters are correctly parsed"
    (are [x y] (= (assoc x :checksum (calculate-checksum y) :uri y)
                  (:imageresizer  (call-handler-with-correct-checksum y)))
         {:original "filename.jpg" 
          :size {:width 300
                 :height 400 }} "size/300x400/filename.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}} "size/300/foo/bar/original.jpg"
         {:original "foo/bar/original.jpg"
          :size {:size 300}
          :rotate {:angle  30}} "rotate/30/size/300/foo/bar/original.jpg"))
  (testing "an invalid checksum should result in status 400 bad request"
    (let [result (handler (request :get "/invalidchecksum/size/300/original.jpg"))]
      (is (= 400 (:status result))))
    (let [result (handler (request :get "/invalidchecksum"))]
      (is (= 400 (:status result))))
    (let [result (handler (request :get "/invalidchecksum/"))]
      (is (= 400 (:status result))))
    (let [result (handler (request :get "/"))]
      (is (= 400 (:status result))))))
