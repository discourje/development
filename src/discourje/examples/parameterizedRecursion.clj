(ns discourje.examples.parameterizedRecursion
  (:require [discourje.core.async :refer :all]
            [discourje.core.logging :refer :all])
  (:import (clojure.lang PersistentArrayMap)))

(def message-exchange-pattern
  (mep
    (rec [:parameterizedRec [:r1 "Alice" :r2 "Bob"]]
         (-->> Integer :r1 :r2)
         (choice [(-->> String :r2 :r1)
                  (continue [:parameterizedRec [:r1 "Bob" :r2 "Alice"]])]
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
    (println response)
    (cond
      (string? response) (do (println "new iteration!")
                             (first-choice-branch r2 r1))
      (number? response) (do (close! alice-to-bob)
                             (close! bob-to-alice)
                             (println "protocol done, and all channels closed.")))))

(defn second-choice-branch [r1 r2]
  (if (== 0 (let [v (<!! r1)] (println v) v))
    (do (>!! r2 "number greater than 0")
        (second-choice-branch r2 r1))
    (>!! r2 1)))

(first-choice-branch alice-to-bob bob-to-alice)
(second-choice-branch alice-to-bob bob-to-alice)