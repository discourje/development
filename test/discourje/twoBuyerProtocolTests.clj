(ns discourje.twoBuyerProtocolTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def alice (createParticipant))
(def bob (createParticipant))

;(macroexpand '(sendOffConsumingInput quoteBook alice))
;(macroexpand '(sendOffFunction quoteBook))

;(sendInput (sendOffFunction generateBook) alice bob)
;(sendInput (sendOffConsumingInput quoteBook bob) bob alice)

(defn setInt [] (int 1))
(defn validateInt [x] (= x 1))

(deftest branchTest
  "tests branching function"
  (let [p1 (createParticipant)
        p2 (createParticipant)]
    (sendInput (sendOffFunction discourje.twoBuyerProtocolTests/setInt) p1 p2)
    (is (= true (branch p2 validateInt)))))

(deftest branchWithBranchesTest
  "tests branching function and evaluates the true branch, which is now just another setInt"
  (let [p1 (createParticipant)
        p2 (createParticipant)]
    (sendInput (sendOffFunction discourje.twoBuyerProtocolTests/setInt) p1 p2)
    (is (= (setInt) (branch p2 validateInt (setInt) "false branch")))))


(deftest sendInputTest
  "tests logic for sendInput"
  (sendInput "hello" alice bob)
  (is (= "hello" (<!! (:input @bob)))))
