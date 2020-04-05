(ns discourje.core.async.spec-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async.spec :as s]))

(defn msg [lts1 lts2]
  (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n"))

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (f))

(defroles (fn [] true))

(use-fixtures :once defroles)

;;;;
;;;; Roles
;;;;

(deftest role-tests
  (is (= (s/role "alice") (s/role "alice")))
  (is (= (s/role x) (s/role x)))
  (is (= (s/role ::alice) (s/role ::alice)))
  (is (= (s/role ::alice 1) (s/role ::alice 1)))
  (is (= (s/role ::alice 1 2) (s/role ::alice 1 2)))
  (is (= (s/role ::alice "foo" "bar") (s/role ::alice "foo" "bar")))
  (is (= (s/role ::alice i) (s/role ::alice i)))
  (is (= (s/role ::alice i j) (s/role ::alice i j)))
  (is (= (s/role ::alice i 1 "foo") (s/role ::alice i 1 "foo")))
  (is (= (s/role ::alice (inc 1)) (s/role ::alice (inc 1))))
  (is (= (s/role ::alice (inc i)) (s/role ::alice (inc i)))))

(role-tests)

;;;;
;;;; Actions
;;;;

(deftest -->>-tests
  (let [lts1 (s/lts (s/-->> ::alice ::bob))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/-->> Integer ::alice ::bob))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Integer,alice,bob)", 1)
                                 (1, "?(Integer,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/-->> (fn [x] (= x 4)) ::alice ::bob))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!((fn [x] (= x 4)),alice,bob)", 1)
                                 (1, "?((fn [x] (= x 4)),alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(-->>-tests)

(deftest close-tests
  (let [lts1 (s/lts (s/close ::alice ::bob))
        lts2 (s/lts (s/aldebaran des (0, 1, 2)
                                 (0, "C(alice,bob)", 1)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/close (::alice "foo") (::bob (inc 1))))
        lts2 (s/lts (s/aldebaran des (0, 1, 2)
                                 (0, "C(alice[\"foo\"],bob[2])", 1)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(close-tests)

;;;;
;;;; Vectors
;;;;

(deftest vector-tests
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/close ::alice ::bob)])
        lts2 (s/lts (s/aldebaran des (0, 3, 4)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "C(alice,bob)", 3)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [[(s/-->> ::alice ::bob)
                      (s/close ::alice ::bob)]])
        lts2 (s/lts (s/aldebaran des (0, 3, 4)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "C(alice,bob)", 3)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::bob)
                     (s/close ::alice ::bob)])
        lts2 (s/lts (s/aldebaran des (0, 5, 6)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,alice,bob)", 3)
                                 (3, "?(Object,alice,bob)", 4)
                                 (4, "C(alice,bob)", 5)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [[(s/-->> ::alice ::bob)
                      (s/-->> ::alice ::bob)]
                     (s/close ::alice ::bob)])
        lts2 (s/lts (s/aldebaran des (0, 5, 6)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,alice,bob)", 3)
                                 (3, "?(Object,alice,bob)", 4)
                                 (4, "C(alice,bob)", 5)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     [(s/-->> ::alice ::bob)
                      (s/close ::alice ::bob)]])
        lts2 (s/lts (s/aldebaran des (0, 5, 6)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,alice,bob)", 3)
                                 (3, "?(Object,alice,bob)", 4)
                                 (4, "C(alice,bob)", 5)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(vector-tests)


;(try

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

;(catch Throwable t (.printStackTrace t)))





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
