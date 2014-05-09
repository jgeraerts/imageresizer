(ns net.umask.imageresizer.vhost_test
  (:use [net.umask.imageresizer.vhost :only [vhost-handler]]
        clojure.test
        ring.mock.request))

(defn- app [request] {:uri (:uri request)})

(def ^:private v (vhost-handler {"localhost" {:handler app}
                                 "blaat.com" {:handler app}}))

(deftest test-vhost
  (testing "an existing host is passed"
    (is (= {:uri "/test"} (v (request :get "http://localhost/test"))))
    (is (= {:uri "/test2"} (v (request :get "http://blaat.com/test2")))))
  (testing "an unknown host is passed"
    (is (= 404 (:status (v (request :get "http://nonexisting.com/blaat"))))))
  (testing "no host header is passed"
    (is (= 404 (:status (v {:uri "/blaat"}))))))
