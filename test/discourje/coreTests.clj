(ns discourje.coreTests
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.core :refer :all]))

(def testingChannel (chan))
(deftest takeTest
  (putMessage testingChannel "hello")
  (is (go (= "hello" (<! (takeMessage testingChannel))))))

(def alice (->participant (thread) (chan) (chan)))
(def bob (->participant (thread) (chan) (chan)))

(deftest provideAlice
  (provide alice "hello alice")
  (is (go (= (str "hello alice") (str (<! (:output alice)))))))

(go (>! (:input bob) "hello bob"))
(go (println (= (str "hello bob") (consume bob (str)))))

(deftest consumeBob
  (go (>! (:input bob) "hello bob"))
  (is (go (= (str "hello bob") (consume bob (str))))))

(deftest messageFromAliceToBob
  (provide alice "message from alice to bob")
  (go (>! (:input bob) (<! (:output alice)))) ; The protocol would take the message from alice output and send to bob input
  (is (go (= (str "message from alice to bob") (consume bob (str))))))