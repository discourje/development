(ns discourje.core.async.spec-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async.spec :as s]))

; Fake constants to avoid "cannot be resolved" warnings
(def des nil)
(def x nil)
(def i nil)
(def j nil)

(defn msg [lts1 lts2]
  (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n"))

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (s/defrole ::carol "carol")
  (s/defrole ::dave "dave")
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
;;;; Multiary operators
;;;;

(deftest choice-tests

  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/aldebaran des (0, 4, 4)
                                 (0, "!(Object,alice,carol)", 1)
                                 (0, "!(Object,alice,bob)", 2)
                                 (1, "?(Object,alice,carol)", 3)
                                 (2, "?(Object,alice,bob)", 3)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/-->> ::alice ::carol)
                              (s/-->> ::alice ::dave)))
        lts2 (s/lts (s/aldebaran des (0, 6, 5)
                                 (0, "!(Object,alice,bob)", 1)
                                 (0, "!(Object,alice,carol)", 2)
                                 (0, "!(Object,alice,dave)", 3)
                                 (1, "?(Object,alice,bob)", 4)
                                 (2, "?(Object,alice,carol)", 4)
                                 (3, "?(Object,alice,dave)", 4)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Idempotence
  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/-->> ::alice ::bob)))
        lts2 (s/lts (s/choice (s/-->> ::alice ::bob)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Commutativity
  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/choice (s/-->> ::alice ::carol)
                              (s/-->> ::alice ::bob)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/choice (s/-->> ::alice ::carol)
                                        (s/-->> ::alice ::dave))))
        lts2 (s/lts (s/choice (s/choice (s/-->> ::alice ::bob)
                                        (s/-->> ::alice ::carol))
                              (s/-->> ::alice ::dave)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/choice (s/-->> ::alice ::carol)
                                        (s/-->> ::alice ::dave))))
        lts2 (s/lts (s/choice (s/-->> ::alice ::bob)
                              (s/-->> ::alice ::carol)
                              (s/-->> ::alice ::dave)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(choice-tests)

(deftest parallel-tests

  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/aldebaran des (0, 12, 9)
                                 (0, "!(Object,alice,bob)", 1)
                                 (0, "!(Object,alice,carol)", 2)
                                 (1, "?(Object,alice,bob)", 3)
                                 (1, "!(Object,alice,carol)", 4)
                                 (2, "!(Object,alice,bob)", 4)
                                 (2, "?(Object,alice,carol)", 8)
                                 (3, "!(Object,alice,carol)", 5)
                                 (4, "?(Object,alice,bob)", 5)
                                 (4, "?(Object,alice,carol)", 7)
                                 (5, "?(Object,alice,carol)", 6)
                                 (7, "?(Object,alice,bob)", 6)
                                 (8, "!(Object,alice,bob)", 7)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)
                                (s/-->> ::alice ::dave)))
        lts2 (s/lts (s/aldebaran des (0, 54, 27)
                                 (0, "!(Object,alice,bob)", 1)
                                 (0, "!(Object,alice,carol)", 2)
                                 (0, "!(Object,alice,dave)", 3)
                                 (1, "?(Object,alice,bob)", 4)
                                 (1, "!(Object,alice,carol)", 5)
                                 (1, "!(Object,alice,dave)", 6)
                                 (2, "!(Object,alice,bob)", 5)
                                 (2, "?(Object,alice,carol)", 21)
                                 (2, "!(Object,alice,dave)", 22)
                                 (3, "!(Object,alice,bob)", 6)
                                 (3, "!(Object,alice,carol)", 22)
                                 (3, "?(Object,alice,dave)", 26)
                                 (4, "!(Object,alice,carol)", 7)
                                 (4, "!(Object,alice,dave)", 8)
                                 (5, "?(Object,alice,bob)", 7)
                                 (5, "?(Object,alice,carol)", 15)
                                 (5, "!(Object,alice,dave)", 16)
                                 (6, "?(Object,alice,bob)", 8)
                                 (6, "!(Object,alice,carol)", 16)
                                 (6, "?(Object,alice,dave)", 20)
                                 (7, "?(Object,alice,carol)", 9)
                                 (7, "!(Object,alice,dave)", 10)
                                 (8, "!(Object,alice,carol)", 10)
                                 (8, "?(Object,alice,dave)", 14)
                                 (9, "!(Object,alice,dave)", 11)
                                 (10, "?(Object,alice,carol)", 11)
                                 (10, "?(Object,alice,dave)", 13)
                                 (11, "?(Object,alice,dave)", 12)
                                 (13, "?(Object,alice,carol)", 12)
                                 (14, "!(Object,alice,carol)", 13)
                                 (15, "?(Object,alice,bob)", 9)
                                 (15, "!(Object,alice,dave)", 17)
                                 (16, "?(Object,alice,bob)", 10)
                                 (16, "?(Object,alice,carol)", 17)
                                 (16, "?(Object,alice,dave)", 19)
                                 (17, "?(Object,alice,bob)", 11)
                                 (17, "?(Object,alice,dave)", 18)
                                 (18, "?(Object,alice,bob)", 12)
                                 (19, "?(Object,alice,bob)", 13)
                                 (19, "?(Object,alice,carol)", 18)
                                 (20, "?(Object,alice,bob)", 14)
                                 (20, "!(Object,alice,carol)", 19)
                                 (21, "!(Object,alice,bob)", 15)
                                 (21, "!(Object,alice,dave)", 23)
                                 (22, "!(Object,alice,bob)", 16)
                                 (22, "?(Object,alice,carol)", 23)
                                 (22, "?(Object,alice,dave)", 25)
                                 (23, "!(Object,alice,bob)", 17)
                                 (23, "?(Object,alice,dave)", 24)
                                 (24, "!(Object,alice,bob)", 18)
                                 (25, "!(Object,alice,bob)", 19)
                                 (25, "?(Object,alice,carol)", 24)
                                 (26, "!(Object,alice,bob)", 20)
                                 (26, "!(Object,alice,carol)", 25)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-idempotence
  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::bob)))
        lts2 (s/lts (s/parallel (s/-->> ::alice ::bob)))]
    (is (s/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Commutativity
  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/parallel (s/-->> ::alice ::carol)
                                (s/-->> ::alice ::bob)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/parallel (s/-->> ::alice ::carol)
                                            (s/-->> ::alice ::dave))))
        lts2 (s/lts (s/parallel (s/parallel (s/-->> ::alice ::bob)
                                            (s/-->> ::alice ::carol))
                                (s/-->> ::alice ::dave)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/parallel (s/-->> ::alice ::carol)
                                            (s/-->> ::alice ::dave))))
        lts2 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)
                                (s/-->> ::alice ::dave)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(parallel-tests)

(deftest vector-tests
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)])
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::carol)])
        lts2 (s/lts (s/aldebaran des (0, 4, 5)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,alice,carol)", 3)
                                 (3, "?(Object,alice,carol)", 4)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::carol)
                     (s/-->> ::alice ::dave)])
        lts2 (s/lts (s/aldebaran des (0, 6, 7)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,alice,carol)", 3)
                                 (3, "?(Object,alice,carol)", 4)
                                 (4, "!(Object,alice,dave)", 5)
                                 (5, "?(Object,alice,dave)", 6)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-idempotence
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::bob)])
        lts2 (s/lts [(s/-->> ::alice ::bob)])]
    (is (s/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-commutativity
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::carol)])
        lts2 (s/lts [(s/-->> ::alice ::carol)
                     (s/-->> ::alice ::bob)])]
    (is (s/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     [(s/-->> ::alice ::carol)
                      (s/-->> ::alice ::dave)]])
        lts2 (s/lts [[(s/-->> ::alice ::bob)
                      (s/-->> ::alice ::carol)]
                     (s/-->> ::alice ::dave)])]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     [(s/-->> ::alice ::carol)
                      (s/-->> ::alice ::dave)]])
        lts2 (s/lts [(s/-->> ::alice ::bob)
                     (s/-->> ::alice ::carol)
                     (s/-->> ::alice ::dave)])]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(vector-tests)

(deftest multiary-tests
  (let [lts1 (s/lts [(s/choice (s/-->> ::alice ::bob)
                               (s/-->> ::alice ::carol))
                     (s/-->> ::alice ::dave)])
        lts2 (s/lts (s/choice [(s/-->> ::alice ::bob)
                               (s/-->> ::alice ::dave)]
                              [(s/-->> ::alice ::carol)
                               (s/-->> ::alice ::dave)]))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts [(s/-->> ::alice ::bob)
                     (s/choice (s/-->> ::alice ::carol)
                               (s/-->> ::alice ::dave))])
        lts2 (s/lts (s/choice [(s/-->> ::alice ::bob)
                               (s/-->> ::alice ::carol)]
                              [(s/-->> ::alice ::bob)
                               (s/-->> ::alice ::dave)]))]
    (is (s/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/parallel (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/choice [(s/-->> ::alice ::bob)
                               (s/-->> ::alice ::carol)]
                              [(s/-->> ::alice ::carol)
                               (s/-->> ::alice ::bob)]))]
    (is (s/not-bisimilar? lts1 lts2) (msg lts1 lts2))))

(multiary-tests)

;;;;
;;;; Conditional operators
;;;;

(deftest if-tests
  (let [lts1 (s/lts (s/if true (s/-->> ::alice ::bob)
                               (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/if false (s/-->> ::alice ::bob)
                                (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,carol)", 1)
                                 (1, "?(Object,alice,carol)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/if (= (inc 1) 2) (s/-->> ::alice ::bob)
                                        (s/-->> ::alice ::carol)))
        lts2 (s/lts (s/aldebaran des (0, 2, 3)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(if-tests)

;;;;
;;;; Recursion operators
;;;;

(deftest loop-recur-tests
  (let [lts1 (s/lts (s/loop swap [r1 ::alice
                                  r2 ::bob]
                            (s/-->> r1 r2)
                            (s/-->> r2 r1)
                            (s/recur swap r2 r1)))
        lts2 (s/lts (s/aldebaran des (0, 8, 8)
                                 (0, "!(Object,alice,bob)", 1)
                                 (1, "?(Object,alice,bob)", 2)
                                 (2, "!(Object,bob,alice)", 3)
                                 (3, "?(Object,bob,alice)", 4)
                                 (4, "!(Object,bob,alice)", 5)
                                 (5, "?(Object,bob,alice)", 6)
                                 (6, "!(Object,alice,bob)", 7)
                                 (7, "?(Object,alice,bob)", 0)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (s/lts (s/loop ring-omega [i 0
                                        n 3]
                            (s/-->> (::alice i) (::alice (mod (inc i) n)))
                            (s/recur ring-omega (mod (inc i) n) n)))
        lts2 (s/lts (s/aldebaran des (0, 7, 7)
                                 (0, "!(Object,alice[0],alice[1])", 1)
                                 (1, "?(Object,alice[0],alice[1])", 2)
                                 (2, "!(Object,alice[1],alice[2])", 3)
                                 (3, "?(Object,alice[1],alice[2])", 4)
                                 (4, "!(Object,alice[2],alice[0])", 5)
                                 (5, "?(Object,alice[2],alice[0])", 0)))]
    (is (s/bisimilar? lts1 lts2) (msg lts1 lts2))))

(loop-recur-tests)

;;;;
;;;; Registry operators
;;;;

;; TODO

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
