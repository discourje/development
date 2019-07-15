(ns discourje.async.messageTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]))

(deftest messageDataTest []
  (let[m (->message "hi" "Hello World")]
    (is (and
          (= "hi" (get-label m))
          (= "Hello World" (get-content m))))))