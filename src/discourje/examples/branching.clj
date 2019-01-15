(ns discourje.examples.branching
  (require [discourje.api.api :refer :all]))

(defn- defineBranchProtocol
  "This function will generate a vector with 3 monitors to send and receive the number message.
  The protocol offers a choice (with internal monitors) to send messages called greaterThan or lessThan to alice depending on the data received"
  []
  [(monitor-send "number" "alice" "bob")
   (monitor-receive "number" "bob" "alice")
   (monitor-choice [(monitor-send "greaterThan" "bob" "alice")
                    (monitor-receive "greaterThan" "alice" "bob")
                    ]
                   [(monitor-send "lessThan" "bob" "alice")
                    (monitor-receive "lessThan" "alice" "bob")
                    ])])

;define the protocol
(def protocol (generateProtocolFromMonitors (defineBranchProtocol)))
;define the participants
(def alice (generateParticipant "alice" protocol))
(def bob (generateParticipant "bob" protocol))

(defn- sendNumberAndAwaitResult
  "This function will use the protocol to send the number message to bob and wait for the result to know if it is greaterThan or lessThan threshold."
  [participant threshold]
  (log (format "%s will now send number." (:name participant)))
  ;We send a map (data structure) in order to send both the threshold and the generated number
  (s! "number" {:threshold threshold :generatedNumber (rand-int (+ threshold 10))} participant "bob")
  (r! ["greaterThan" "lessThan"] "bob" participant
              (fn [response]
                (cond
                  (= response "Greater!") (log (format "greaterThan received by %s" (:name participant)))
                  (= response "Smaller!") (log (format "lessThan received by %s" (:name participant)))))))

(defn- receiveNumber
  "This function will use the protocol to listen for the number message. Check the number and threshold and send result"
  [participant]
  (r! "number" "alice" participant
              (fn [numberMap]
                (let [threshold (:threshold numberMap)
                      generated (:generatedNumber numberMap)]
                  (log (:name participant) "received " numberMap)
                  (if (> generated threshold)
                    (s! "greaterThan" "Greater!" participant "alice")
                    (s! "lessThan" "Smaller!" participant  "alice"))))))

;start the `GreetBobAndCarol' function on thread and add `alice' participant
(clojure.core.async/thread (sendNumberAndAwaitResult alice 10))
;start the `receiveGreet' function on thread and add `bob' participant
(clojure.core.async/thread (receiveNumber bob))
