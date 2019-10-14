(ns discourje.examples.branching
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

; This function will generate a mep to send and receive the number message.
; The mep offers a choice (with internal interactions) to send messages called greaterThan or lessThan to alice depending on the data received
(def message-exchange-pattern
  (mep (-->> "number" "alice" "bob")
       (choice [(-->> "greaterThan" "bob" "alice")]
               [(-->> "lessThan" "bob" "alice")])
       (close "alice" "bob")
       (close "bob" "alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "alice" "bob"))
(def bob-to-alice (get-channel infrastructure "bob" "alice"))

(defn- send-number-and-await-result
  "This function will use the protocol to send the number message to bob and wait for the result to know if it is greaterThan or lessThan threshold."
  [threshold]
  ;We send a map (data structure) in order to send both the threshold and the generated number
  (>!! alice-to-bob {msg "number" {:threshold threshold :generatedNumber (rand-int (+ threshold 10))}})
  (let [response (<!! bob-to-alice ["greaterThan" "lessThan"])]
    (cond
      (= (:flag response) "greaterThan") (log-message (format "greaterThan received with message: %s"(:content response)))
      (= (:flag response) "lessThan") (log-message (format "lessThan received with message: %s" (:content response))))
    (close! "alice" "bob" infrastructure)
    (close! bob-to-alice)))

(defn- receive-number
  "This function will use the protocol to listen for the number message. Check the number and threshold and send result"
  []
  (let [numberMap (<!! alice-to-bob "number")
        threshold (:threshold  numberMap)
        generated (:generatedNumber  numberMap)]
    (if (> generated threshold)
      (>!! bob-to-alice (msg "greaterThan" {:flag "greaterThan" :content "Number send is greater!"}))
      (>!! bob-to-alice (msg "lessThan" {:flag "lessThan" :content "Number send is smaller!"})))))

;start the `sendNumberAndAwaitResult' function on thread and supply some threshold
(clojure.core.async/thread (send-number-and-await-result 10))
;start the `receiveGreet' function on thread
(clojure.core.async/thread (receive-number))
