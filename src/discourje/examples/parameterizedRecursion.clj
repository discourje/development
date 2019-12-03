(ns discourje.examples.parameterizedRecursion
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

; The protocol below describes a flow where alice and bob continue to switch positions.
; The implementation shows how they continually exchange a random number, if the number == 0 the protocol loops
; If not, the protocol stops and the channels are closed
(def message-exchange-pattern
  (mep
    (rec [:parameterizedRec [:r1 "Alice" :r2 "Bob"]]
         (-->> Integer :r1 :r2)
         (choice [(-->> String :r2 :r1)
                  (continue [:parameterizedRec [:r2 :r1]])]
                 [(-->> Long :r2 :r1)]))
    (close "Alice" "Bob")
    (close "Bob" "Alice")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "Alice" "Bob"))
(def bob-to-alice (get-channel infrastructure "Bob" "Alice"))

(defn first-choice-branch [r1 r2]
  (>!! r1 (rand-int 2))
  (let [response (<!! r2)]
    (cond
      (string? response) (do (println "new iteration!")
                             (first-choice-branch r2 r1))
      (number? response) (do (close! alice-to-bob)
                             (close! bob-to-alice)
                             (println "protocol done, and all channels closed.")))))

; Notice the short delay (Thread sleep 500) in here since the recursive call of the function is faster than the send of the message.
; So without delay, the protocol fails because the roles are switched.
; This is a faulty implementation, but demonstrates an issue that can come up when using parameterized recursion!
(defn second-choice-branch [r1 r2]
  (if (== 0 (<!! r1))
    (do (>!! r2 "number greater than 0")
        (Thread/sleep 500)
        (second-choice-branch r2 r1))
    (>!! r2 1)))

(thread (first-choice-branch alice-to-bob bob-to-alice))
(thread (second-choice-branch alice-to-bob bob-to-alice))