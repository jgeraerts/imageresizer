(ns net.umask.imageresizer.config_test
  (:require [clojure.test :refer :all]
            [net.umask.imageresizer.config :refer :all]))

(deftest test-create-vhostmap
  (testing "test maps correctly transformed with vector single element"
    (let [result (make-vhostmap [["localhost"] identity])]
      (is (contains? result "localhost"))))
  (testing "Make vhost map with vector with multiple elements"
    (let [result (make-vhostmap [["foo" "bar"] identity])]
      (is (contains? result "foo"))
      (is (contains? result "bar"))))
  (testing "Make vhost map with string only"
    (let [result (make-vhostmap ["localhost" identity])]
      (is (contains? result "localhost"))))
  (testing "make vhost map with empty vector and function"
    (let [result (make-vhostmap [[] identity])]
      (is (= {} result))))
  (testing "Make vhost map with empty vector"
    (is (= {} (make-vhostmap []))))
  (testing "invalid parameters"
    (is (thrown? AssertionError (make-vhostmap ["blaat"])))))


