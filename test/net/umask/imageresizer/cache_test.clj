(ns net.umask.imageresizer.cache-test
  (:require [net.umask.imageresizer.cache :as c]
            [ring.util.response :refer [response not-found]]
            [clojure.test :refer :all]
            [ring.mock.request :refer :all]))

(def cached-response {:body "cached" :status 200 :headers {}})

(def handler-response (response "from-handler"))

(def notfound-response (not-found "item not found"))

(defn memory-store
  [store]
  (reify
    c/CacheProtocol
    (fetch [this name] (get @store name))
    (store! [this name response] (swap! store assoc name response))))

(defn- create-handler
  [store]
  (c/wrap-cache store
                (fn [request]
                  (condp = (:uri request)
                    "/notcached" handler-response
                    "/cached"    handler-response
                    "/not-found" notfound-response))))

(deftest test-cache
  (let [store (memory-store (atom {}))        
        handler (create-handler store)]
    (c/store! store "/cached" cached-response)
    (testing "the handler should be invoked when the item is not in cache. "
      (is (= handler-response (handler (request :get "/notcached"))))
      (is (= handler-response (c/fetch store "/notcached"))))
    (testing "the handler should not be invoked when the item is in cache"
      (is (= cached-response (handler (request :get "/cached")))))
    (testing "the handler should be invoked when a Cache-Control: no-cache request header is send."
      (is (= handler-response (handler (-> (request :get "/cached" )
                                           (header "Cache-Control" "no-cache")))))
      (is (= handler-response (c/fetch store "/cached"))))
    (testing "non-200 responses should not be cached"
      (is (= notfound-response (handler (request :get "/not-found"))))
      (is (nil? (c/fetch store "/not-found"))))))
