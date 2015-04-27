(ns net.umask.imageresizer.checksum-test
  (:require [clojure.test :refer :all]
            [net.umask.imageresizer.checksum :refer :all]
            [ring.util.response :refer [response not-found]]
            [ring.mock.request :refer :all]
            digest))

(defn- addchecksum [secret url]
  (str "/" (digest/md5 (str secret url)) "/" url))


(defn- test-handler [request]
  (let [uri (:uri request)]
    (if (= "/some/url" uri)
      (response "success")
      (not-found (str "fail. requested uri: " uri)))))

(deftest test-checksum
  (let [secret "secret"
        handler (wrap-checksum secret test-handler)]
    (testing "invalid checksum should give 400"
      (is (= 400 (:status (handler (request :get "/")))))
      (is (= 400 (:status (handler (request :get "/invalid")))))
      (is (= 400 (:status (handler (request :get "/invalid/")))))
      (is (= 400 (:status (handler (request :get "/invalid/some/url"))))))
    (testing "a correct checksum should just pass the request to the handler"
      (is (= {:status 200
              :body "success"
              :headers {}} (handler (request :get (addchecksum secret "some/url"))))))))
