(ns net.umask.imageresizer.expires-test
  (:require
   [clojure.test :refer :all]
   [clj-time.core :as t]
   [clj-time.coerce :refer [to-long]]
   [net.umask.imageresizer.expires :refer [wrap-expires expire-option]]
   [net.umask.imageresizer.urlparser :refer [option-from-url]]))


(def handler (wrap-expires identity))

(defn- expires-link [expires]
  (str "/expires/"  expires  "/foo.jpg"))

(deftest test-expires  
  (t/do-at
   (t/date-time 2015 12 01)
   (is (= 404 (:status (handler {:uri (expires-link (to-long (t/date-time 2015 11 30)))}))))
   (is (= {:uri "/foo.jpg"
           :headers
           {"expires" "Thu, 03 Dec 2015 00:00:00 GMT",
            "cache-control" "max-age=172800"}}
          (handler {:uri (expires-link (to-long (t/date-time 2015 12 03)))})))
   (is (= {:uri "/foo.jpg"
           :headers
           {"expires" "Thu, 31 Dec 2099 00:00:00 GMT",
            "cache-control" "max-age=2653430400"}}
          (handler {:uri "/foo.jpg"})))))

(deftest test-parse-expire-options
  (are [x y] (= x (option-from-url y expire-option))
    {:uri "/foo.jpg" :expires-at 123} "/expires/123/foo.jpg"))
