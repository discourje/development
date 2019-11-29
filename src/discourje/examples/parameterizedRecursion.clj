(ns discourje.examples.parameterizedRecursion
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

(def message-exchange-pattern
  (mep
    (rec [:parameterizedRec [:r1 "Alice" :r2 "Bob" :r3 "Carol" :r4 "Fred"]]
         (-->> Integer :r3 :r4)
         (choice [(-->> String :r2 :r1)
                  (continue [:parameterizedRec [:r2 :r1 :r3 :r4]])]
                 [(-->> Long :r2 :r1)]))
    (close "Alice" "Bob")
    (close "Bob" "Alice")
    (close "Carol" "Fred")))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel infrastructure "Alice" "Bob"))
(def bob-to-alice (get-channel infrastructure "Bob" "Alice"))
(def carol-to-fred (get-channel infrastructure "Carol" "Fred"))

(declare second-choice-branch)

(defn first-choice-branch [r1 r2 r3]
  (>!! r3 (rand-int 2))
  (let [response (<!! r2)]
    (cond
      (string? response) (do (println "new iteration!")
                             (first-choice-branch r2 r1 r3)
                             )
      (number? response) (do (close! alice-to-bob)
                             (close! bob-to-alice)
                             (close! carol-to-fred)
                             (println "protocol done, and all channels closed.")))))

(defn second-choice-branch [r1 r2 r3]
  (if (== 0 (<!! r3))
    (do (>!! r2 "number == 0")
        (Thread/sleep 1000)
        (second-choice-branch r2 r1 r3)
        )
    (>!! r2 1)))

(thread (first-choice-branch alice-to-bob bob-to-alice carol-to-fred))
(thread (second-choice-branch alice-to-bob bob-to-alice carol-to-fred))