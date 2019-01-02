(ns discourje.examples.recursion
  (require [discourje.core.monitor :refer :all]
           [discourje.core.core :refer :all]
           [discourje.core.protocol :refer :all]
           [discourje.core.dataStructures :refer :all]))

(defn- defineRecursiveProtocol
  "This function will generate a vector with 3 monitors to send and receive the number message.
  The protocol offers a choice (with internal monitors) to send messages called greaterThan or lessThan to alice depending on the data received
  When the number is greater than the threshold, the protocol will recur, if not the protocol will stop.
  Recur is matched by name, in this case: :generateNumbers"
  []
  (vector
    (->recursion :generateNumbers
                 (vector
                   (->sendM "number" "alice" "bob")
                   (->receiveM "number" "bob" "alice")
                   (->choice [(->sendM "greaterThan" "bob" "alice")
                              (->receiveM "greaterThan" "alice" "bob")
                              (generateRecur :generateNumbers)]
                             [(->sendM "lessThan" "bob" "alice")
                              (->receiveM "lessThan" "alice" "bob")
                              (generateRecurStop :generateNumbers)])))))

(defn generateRecursiveProtocol
  "Generate the protocol, channels and set the first monitor active."
  []
  (generateProtocol ["alice" "bob"] (defineRecursiveProtocol)))

;define the protocol
(def protocol (atom (generateRecursiveProtocol)))
;define the participants
(def alice (discourje.core.core/->participant "alice" protocol))
(def bob (discourje.core.core/->participant "bob" protocol))

(defn- displayResult
  "print the result to the REPL"
  [result]
  (println result))

(defn- sendNumberAndAwaitResult
  "This function will use the protocol to send the number message to bob and wait for the result to know if it is greaterThan or lessThan threshold.
   Notice, when the number is greater than, the function will call itself"
  [participant threshold]
  (println (format "%s will now send number." (:name participant)))
  ;We send a map (data structure) in order to send both the threshold and the generated number
  (send-to participant "number" {:threshold threshold :generatedNumber (rand-int (+ threshold 10))} "bob")
  (receive-by participant ["greaterThan" "lessThan"] "bob"
              (fn [response]
                (cond
                  (= response "Greater!") (do (displayResult (format "greaterThan received by %s" (:name participant)))
                                              (sendNumberAndAwaitResult participant threshold))
                  (= response "Smaller!") (displayResult (format "lessThan received by %s" (:name participant)))))))

(defn- receiveNumber
  "This function will use the protocol to listen for the number message. Check the number and threshold and send result
  Notice, when the number is greater than, the function will call itself"
  [participant]
  (receive-by participant "number" "alice"
              (fn [numberMap]
                (let [threshold (:threshold numberMap)
                      generated (:generatedNumber numberMap)]
                  (if (> threshold generated)
                    (do (send-to participant "greaterThan" "Greater!" "alice")
                        (receiveNumber participant))
                    (send-to participant "lessThan" "Smaller!" "alice"))))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (sendNumberAndAwaitResult alice 10))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveNumber bob))