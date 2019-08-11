(ns discourje.examples.parallelization
  (require [discourje.core.async :refer :all]
           [discourje.core.logging :refer :all]))

; parallelization allows multiple paths to be traversed.
; The protocol below demonstrates how Bob can send both the message labelled 2 and 4 to Alice at the `same' time.
; Alose, Alice will be able to respond to Bob with message 3 and 5 at the `same' time.
(def message-exchange-pattern
  (mep (-->> 1 "Alice" "Bob")
       (par [(-->> 2 "Bob" "Carol")
             (-->> 3 "Carol" "Bob")]
            [(-->> 4 "Bob" "Alice")
             (-->> 5 "Alice" "Bob")])))

;setup infrastructure, generate channels and add monitor
(def infrastructure (add-infrastructure message-exchange-pattern))
;Get the channels
(def alice-to-bob (get-channel "Alice" "Bob" infrastructure))
(def bob-to-alice (get-channel "Bob" "Alice" infrastructure))
(def bob-to-carol (get-channel "Bob" "Carol" infrastructure))
(def carol-to-bob (get-channel "Carol" "Bob" infrastructure))

(defn- alice
  "logic for alice"
  []
  (>!! alice-to-bob (msg 1 1))
  (<!! bob-to-alice 4)
  (>!! alice-to-bob (msg 5 5)))

(defn- bob
  "logic for bob, notice how the parallel branches are started on separate threads to demonstrate both branches can be traversed at the same time"
  []
  (<!! alice-to-bob 1)
  (let [first-par (fn []
                    (>!! bob-to-carol (msg 2 2))
                    (<!! carol-to-bob 3))
        second-par (fn []
                     (>!! bob-to-alice (msg 4 4))
                     (<!! alice-to-bob 5))]
    (clojure.core.async/thread (first-par))
    (clojure.core.async/thread (second-par))))

(defn- carol
  "logic for carol"
  []
  (<!! bob-to-carol 2)
  (>!! carol-to-bob (msg 3 3)))

;start the `alice' function on thread
(clojure.core.async/thread (alice))
;start the `bob' function on thread and add the channel
(clojure.core.async/thread (bob))
;start the `carol' function on thread and add the channel
(clojure.core.async/thread (carol))