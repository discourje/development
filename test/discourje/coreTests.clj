(ns discourje.coreTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def testingChannel (chan))
(def alice (createParticipant))
(def bob (createParticipant))

(def testThread (thread ))

(deftest takeTest
  (putMessage testingChannel "hello")
  (is (go (= "hello" (<! (takeMessage testingChannel))))))

(deftest blockingPutMessageAlice
  (dataToInput @alice "hey Alice")
  (is (= "hey Alice" (<!! (:input @alice)))))

(deftest provideAlice
  (provide @alice "hello alice")
  (is (go (= (str "hello alice") (str (<! (:output @alice)))))))

(deftest consumeBob
  (go (>! (:input @bob) "hello bob"))
  (is (go (= (str "hello bob") (<! (consume @bob str))))))

(deftest messageFromAliceToBob
  (provide @alice "message from alice to bob")
  (fromOutputToInput @alice @bob)             ; The protocol would take the message from alice output and send to bob input
  (is (go (= (str "message from alice to bob") (<! (consume @bob str))))))

(deftest messageFromAliceToBobInputToOutput
  (provide @alice "message from alice to bob")
  (fromOutputToInput @alice @bob)                  ; The protocol would take the message from alice output and send to bob input
  (is (go (= (str "message from alice to bob") (<! (consume @bob str))))))

(deftest toUpper
  (is (= "TEST" (clojure.string/upper-case "test"))))

;(def a (chan))
;(go (>! a "aaa"))
;(go (println (take! (<! (thread (go (<! a)))) clojure.string/upper-case)))

;(go
;  (println
;    (clojure.string/upper-case
;      (<! (thread
;            (<!! (go
;                   (<! a))))))))

(deftest getValueFromThread
  (let [a (chan)]
  (go (>! a "aaa"))
  (is (go (= "AAA" (clojure.string/upper-case (<! (thread (<!! (go (<! a)))))))))))

;(go ;this should work
;  (println
;    (= "AAA"
;       (clojure.string/upper-case
;         (<! (thread
;               (<!! (go
;                      (<! a)))))))))

;(go (println (take! (<! (thread (go (<! a)))) clojure.string/upper-case)))



(go (>! (:input @bob) "test"))
(go (println (<! (consume @bob str :test))))


(deftest messageFromAliceToBobOnThread
  (provide @alice "message from alice to bob")
  (fromOutputToInput @alice @bob)     ; The protocol would take the message from alice output and send to bob input
  (is (go (= (str "message from alice to bob") (<! (consume @bob str :test))))))

(deftest sendMessageFromAliceToBobOnThread
  (sendMessage @alice @bob "message from alice to bob")
  (is (go (= (str "message from alice to bob") (<! (consume @bob str :test))))))