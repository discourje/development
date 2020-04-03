(ns discourje.core.async.spec-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async.spec :as s]))

(deftest spec-tests

  ;; Action

  (let [lts1 (s/lts (s/-->> "alice" "bob"))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(alice,bob,Object)", 1)
                                 (1, "?(alice,bob,Object)", 2)))]
    (is (s/bisimilar? lts1 lts2)
        (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n")))

  (let [lts1 (s/lts (s/-->> Long "alice" "bob"))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(alice,bob,Long)", 1)
                                 (1, "?(alice,bob,Long)", 2)))]
    (is (s/bisimilar? lts1 lts2)
        (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n")))

  (let [lts1 (s/lts (s/close "alice" "bob"))
        lts2 (s/lts (s/aldebaran des (0, 1, 2)
                                 (0, "C(alice,bob)", 1)))]
    (is (s/bisimilar? lts1 lts2)
        (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n")))

  ;; Sequence

  (let [lts1 (s/lts [(s/-->> "alice" "bob")
                     (s/close "alice" "bob")])
        lts2 (s/lts (s/aldebaran des (0, 3, 4)
                                 (0, "!(alice,bob,Object)", 1)
                                 (1, "?(alice,bob,Object)", 2)
                                 (2, "C(alice,bob)", 3)))]
    (is (s/bisimilar? lts1 lts2)
        (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n")))
  )

(spec-tests)


(try

  ;(def spec (s/choice (s/-->> Long "alice" "bob")
  ;                    (s/close "alice" "bob")))
  ;
  ;(def spec (s/choice (s/choice [(s/-->> Long "alice" "bob")
  ;                               (s/close "alice" "bob")]
  ;                              (s/-->> Long "alice" "carol"))
  ;                    (s/close "alice" "bob")))
  ;
  ;(def spec (s/parallel (s/close "alice" "bob")
  ;                      (s/close "alice" "carol")
  ;                      (s/close "alice" "dave")))
  ;
  ;(def spec (s/parallel (s/-->> Long "alice" "bob")
  ;                      (s/-->> Long "alice" "carol")))
  ;
  ;(def spec (s/loop ring [i 0
  ;                        n 4]
  ;                  (s/-->> Long (alice i) (alice (mod (inc i) n)))
  ;                  (s/recur ring (mod (inc i) n) n)))

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



  ;(def lts (s/lts spec true))
  ;(s/println lts)
  ;(s/ltsgraph lts "/Applications/mCRL2.app/Contents" "/Users/sungshik/Desktop/lts.aut")

  (catch Throwable t (.printStackTrace t)))





;(try
;  (def ast (s/aldebaran des (0, 9, 9)
;                        (0, "!(alice[0],alice[1],Long)", 1)
;                        (1, "?(alice[0],alice[1],Long)", 2)
;                        (2, "!(alice[1],alice[2],Long)", 3)
;                        (3, "?(alice[1],alice[2],Long)", 4)
;                        (4, "!(alice[2],alice[3],Long)", 5)
;                        (5, "?(alice[2],alice[3],Long)", 6)
;                        (6, "!(alice[3],alice[0],Long)", 7)
;                        (7, "?(alice[3],alice[0],Long)", 8)
;                        (8, "!(alice[0],alice[1],Long)", 1)))
;  (println ast)
;
;  (def lts (s/lts ast true))
;  (s/println lts)
;  (s/ltsgraph lts "/Applications/mCRL2.app/Contents" "/Users/sungshik/Desktop/lts.aut")
;
;  (catch Throwable t (.printStackTrace t)))
