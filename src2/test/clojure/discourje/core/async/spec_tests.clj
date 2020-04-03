(ns discourje.core.async.spec-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async.spec :as s]))

(def alice (s/role "alice"))
(def bob (s/role "bob"))

(try
  (def spec [(s/-->> Long "alice" "bob")
             (s/close "alice" "bob")])

  (def spec (s/choice (s/-->> Long "alice" "bob")
                      (s/close "alice" "bob")))

  (def spec (s/choice (s/choice [(s/-->> Long "alice" "bob")
                                 (s/close "alice" "bob")]
                                (s/-->> Long "alice" "carol"))
                      (s/close "alice" "bob")))

  (def spec (s/parallel (s/close "alice" "bob")
                        (s/close "alice" "carol")
                        (s/close "alice" "dave")))

  (def spec (s/parallel (s/-->> Long "alice" "bob")
                        (s/-->> Long "alice" "carol")))

  (def spec (s/loop ring [i 0
                          n 4]
                    (s/-->> Long (alice i) (alice (mod (inc i) n)))
                    (s/recur ring (mod (inc i) n) n)))

  ;(def spec (s/loop swap [r1 "alice"
  ;                        r2 "bob"]
  ;                  (s/-->> Long r1 r2)
  ;                  (s/-->> Long r2 r1)
  ;                  (s/recur swap r2 r1)))

  ;(def spec (s/loop pipe [i 0
  ;                        n 4]
  ;                  (s/if (< i n)
  ;                    [(s/-->> Long (alice i) (alice (inc i)))
  ;                     (s/recur pipe (inc i) n)])))

  ;(s/def :pipe
  ;  [role min max]
  ;  (s/loop pipe [i min]
  ;          (s/if (< i max)
  ;            [(s/-->> Long (role i) (role (inc i)))
  ;             (s/recur pipe (inc i))])))
  ;
  ;(s/def :pipe
  ;  [role max]
  ;  (s/apply :pipe [alice 0 max]))
  ;
  ;(def spec (s/apply :pipe [alice 2]))

  ;(def spec (s/apply :repeat [1 (s/-->> (alice 2) (bob 4))]))

  (def lts (s/lts spec true))

  (s/println lts)
  (s/ltsgraph lts "/Applications/mCRL2.app/Contents" "/Users/sungshik/Desktop/lts.aut")

  (catch Throwable t (.printStackTrace t)))