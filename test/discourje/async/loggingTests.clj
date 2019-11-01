(ns discourje.async.loggingTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all]))

(deftest when-set-throwing-exceptions-can-be-thrown
  (set-throwing true)
  (set-logging-and-exceptions)
  (is (= true (can-throw?))))

(deftest when-set-logging-can-be-done
  (set-logging)
  (is (= true (can-log?))))

(deftest when-set-logging-and-exceptions-can-logging-be-done
  (set-logging-and-exceptions)
  (is (= true (can-log?))))

(deftest when-set-logging-and-exceptions-can-logging-NOT-be-done
  (set-logging-exceptions)
  (is (= false (can-log?))))

(deftest when-set-logging-and-exceptions-can-exceptions-NOT-be-done
  (set-logging)
  (is (= false (can-throw?))))

(deftest throwing-java-exception-works-properly-without-slingshot-library
  (set-logging-and-exceptions)
  (set-throwing true)
  (is (thrown? Exception (log-error :test-exception "this is a test exception"))))