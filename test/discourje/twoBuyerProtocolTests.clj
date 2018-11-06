(ns discourje.twoBuyerProtocolTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]
            [discourje.twoBuyerProtocol :refer :all]))

(def alice (createParticipant))
(def bob (createParticipant))

(deftest sendInputTest
  "tests logic for sendInput"
  (sendInput "hello" alice bob)
  (is (= "hello" (<!! (:input @bob)))))
