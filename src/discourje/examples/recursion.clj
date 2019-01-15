(ns discourje.examples.recursion
  (require [discourje.api.api :refer :all]))

(defn- defineRecursiveProtocol
  "This function will generate a vector with 3 monitors to send and receive the number message.
  The protocol offers a choice (with internal monitors) to send messages called greaterThan or lessThan to alice depending on the data received
  When the number is greater than the threshold, the protocol will recur, if not the protocol will stop.
  Recur is matched by name, in this case: :generateNumbers"
  []
  [(monitor-recursion :generateNumbers [
                   (monitor-send "number" "alice" "bob")
                   (monitor-receive "number" "bob" "alice")
                   (monitor-choice [(monitor-send "greaterThan" "bob" "alice")
                              (monitor-receive "greaterThan" "alice" "bob")
                              (do-recur :generateNumbers)]
                             [(monitor-send "lessThan" "bob" "alice")
                              (monitor-receive "lessThan" "alice" "bob")
                              (do-end-recur :generateNumbers)])])])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineRecursiveProtocol)))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))

(defn- sendNumberAndAwaitResult
  "This function will use the protocol to send the number message to bob and wait for the result to know if it is greaterThan or lessThan threshold.
   Notice, when the number is greater than, the function will call itself"
  [participant threshold]
  (log (format "%s will now send number." (:name participant)))
  ;We send a map (data structure) in order to send both the threshold and the generated number
  (s! "number" {:threshold threshold :generatedNumber (rand-int (+ threshold 10))} participant "bob")
  (r! ["greaterThan" "lessThan"] "bob" participant
              (fn [response]
                (cond
                  (= response "Greater!") (do (log (format "greaterThan received by %s" (:name participant)))
                                              (sendNumberAndAwaitResult participant threshold))
                  (= response "Smaller!") (log (format "lessThan received by %s" (:name participant)))))))

(defn- receiveNumber
  "This function will use the protocol to listen for the number message. Check the number and threshold and send result
  Notice, when the number is greater than, the function will call itself"
  [participant]
  (r! "number" "alice" participant
              (fn [numberMap]
                (let [threshold (:threshold numberMap)
                      generated (:generatedNumber numberMap)]
                  (if (> generated threshold)
                    (do (s! "greaterThan" "Greater!" participant "alice")
                        (receiveNumber participant))
                    (s! "lessThan" "Smaller!" participant "alice"))))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (sendNumberAndAwaitResult alice 10))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveNumber bob))