(ns discourje.examples.recursion
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

;  This function will generate a mep with 1 recursion to send and receive the number message (recursion).
;  The protocol offers a choice (with internal interactions) to send messages called greaterThan or lessThan to alice depending on the data received
;  When the number is greater than the threshold, the protocol will recur, if not the protocol will stop.
;  Recur is matched by name, in this case: :generate
(def message-exchange-pattern
  (mep (rec :generate
            (-->> "number" "alice" "bob")
            (choice
              [(-->> "greaterThan" "bob" "alice")
               (continue :generate)]
              [(-->> "lessThan" "bob" "alice")]))))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "alice" "bob"))
(def bob-to-alice (get-channel infrastructure "bob" "alice"))

(defn- send-number-and-await-result
  "This function will use the protocol to send the number message to bob and wait for the result to know if it is greaterThan or lessThan threshold."
  [threshold]
  ;We send a map (data structure) in order to send both the threshold and the generated number
  (>!! alice-to-bob (msg "number" {:threshold threshold :generatedNumber (rand-int (+ threshold 10))}))
  (let [response (<!! bob-to-alice ["greaterThan" "lessThan"])]
    (cond
      (= (get-label response) "greaterThan")
      (do (log-message (format "greaterThan received with message: %s" (get-content response)))
          (send-number-and-await-result threshold))
      (= (get-label response) "lessThan")
      (log-message (format "lessThan received with message: %s" (get-content response))))))

(defn- receive-number
  "This function will use the protocol to listen for the number message. Check the number and threshold and send result"
  []
  (let [numberMap (<!! alice-to-bob "number")
        threshold (:threshold (get-content numberMap))
        generated (:generatedNumber (get-content numberMap))]
    (if (> generated threshold)
      (do (>!! bob-to-alice (msg "greaterThan" "Number send is greater!"))
          (receive-number))
      (>!! bob-to-alice (msg "lessThan" "Number send is smaller!")))))

;start the `send-number-and-await-result' function on thread and supply some threshold
(clojure.core.async/thread (send-number-and-await-result 5))
;start the `receive-number' function on thread
(clojure.core.async/thread (receive-number))
