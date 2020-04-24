(ns discourje.spec-tests
  (:require [clojure.test :refer :all]
            [discourje.spec :as s]
            [discourje.spec.lts :as lts]))

(defn msg [lts1 lts2]
  (str "\n *** lts1 ***\n\n" lts1 "\n\n *** lts2 ***\n\n" lts2 "\n"))

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (s/defrole ::carol "carol")
  (s/defrole ::dave "dave")
  (f))

(defroles (fn [] true))

(def x nil)
(def i nil)
(def j nil)
(def n nil)
(def des nil)
(def r1 nil)
(def r2 nil)
(def r-name nil)
(def swap nil)
(def pipe nil)
(def ring nil)
(def multicast nil)
(def anycast nil)
(def omega nil)

(use-fixtures :once defroles)

;;;;
;;;; Roles
;;;;

(deftest role-tests
  (is (= (s/role "alice") (s/role "alice")))
  (is (= (s/role x []) (s/role x [])))
  (is (= (s/role ::alice) (s/role ::alice)))
  (is (= (s/role ::alice [1]) (s/role ::alice [1])))
  (is (= (s/role ::alice [1 2]) (s/role ::alice [1 2])))
  (is (= (s/role ::alice [i]) (s/role ::alice [i])))
  (is (= (s/role ::alice [i j]) (s/role ::alice [i j])))
  (is (= (s/role ::alice [(inc 1)]) (s/role ::alice [(inc 1)])))
  (is (= (s/role ::alice [(inc i)]) (s/role ::alice [(inc i)])))

  (let [x 1
        y (inc 1)
        z (inc y)]
    (is (= (s/role ::alice [x]) (s/role ::alice [1])))
    (is (= (s/role ::alice [(inc x)]) (s/role ::alice [(inc 1)])))
    (is (= (s/role ::alice [y]) (s/role ::alice [2])))
    (is (= (s/role ::alice [z]) (s/role ::alice [3])))))

(role-tests)

;;;;
;;;; Actions
;;;;

(deftest -->-tests
  (let [lts1 (lts/lts (s/--> ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 1, 2)
                                   (0, "‽(Object,alice,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/--> Integer ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 1, 2)
                                   (0, "‽(Integer,alice,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/--> (fn [x] (= x 4)) ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 1, 2)
                                   (0, "‽((fn [x] (= x 4)),alice,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(deftest -->>-tests
  (let [lts1 (lts/lts (s/-->> ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/-->> Integer ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Integer,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/-->> (fn [x] (= x 4)) ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!((fn [x] (= x 4)),alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(-->>-tests)

(deftest close-tests
  (let [lts1 (lts/lts (s/close ::alice ::bob))
        lts2 (lts/lts (s/aldebaran des (0, 1, 2)
                                   (0, "C(alice,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/close (::alice 1) (::bob (inc 1))))
        lts2 (lts/lts (s/aldebaran des (0, 1, 2)
                                   (0, "C(alice[1],bob[2])", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(close-tests)

;;;;
;;;; Nullary operators
;;;;

(deftest any-tests
  (let [lts1 (lts/lts (s/any #{::alice ::bob}))
        lts2 (lts/lts (s/aldebaran des (0, 8, 2)
                                   (0, "‽(Object,alice,bob)", 1)
                                   (0, "!(Object,alice,bob)", 1)
                                   (0, "?(alice,bob)", 1)
                                   (0, "C(alice,bob)", 1)
                                   (0, "‽(Object,bob,alice)", 1)
                                   (0, "!(Object,bob,alice)", 1)
                                   (0, "?(bob,alice)", 1)
                                   (0, "C(bob,alice)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/any #{::alice ::bob ::carol}))
        lts2 (lts/lts (s/aldebaran des (0, 24, 2)
                                   (0, "‽(Object,alice,bob)", 1)
                                   (0, "C(alice,bob)", 1)
                                   (0, "!(Object,alice,bob)", 1)
                                   (0, "?(alice,bob)", 1)
                                   (0, "‽(Object,alice,carol)", 1)
                                   (0, "!(Object,alice,carol)", 1)
                                   (0, "?(alice,carol)", 1)
                                   (0, "C(alice,carol)", 1)
                                   (0, "!(Object,bob,alice)", 1)
                                   (0, "‽(Object,bob,alice)", 1)
                                   (0, "?(bob,alice)", 1)
                                   (0, "C(bob,alice)", 1)
                                   (0, "‽(Object,bob,carol)", 1)
                                   (0, "!(Object,bob,carol)", 1)
                                   (0, "?(bob,carol)", 1)
                                   (0, "C(bob,carol)", 1)
                                   (0, "‽(Object,carol,alice)", 1)
                                   (0, "!(Object,carol,alice)", 1)
                                   (0, "?(carol,alice)", 1)
                                   (0, "C(carol,alice)", 1)
                                   (0, "‽(Object,carol,bob)", 1)
                                   (0, "!(Object,carol,bob)", 1)
                                   (0, "?(carol,bob)", 1)
                                   (0, "C(carol,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(any-tests)

;;;;
;;;; Multiary operators
;;;;

(deftest cat-tests
  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 4, 5)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,alice,carol)", 3)
                                   (3, "?(alice,carol)", 4)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))
        lts2 (lts/lts (s/aldebaran des (0, 6, 7)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,alice,carol)", 3)
                                   (3, "?(alice,carol)", 4)
                                   (4, "!(Object,alice,dave)", 5)
                                   (5, "?(alice,dave)", 6)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-idempotence
  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/cat (s/-->> ::alice ::bob)))]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-commutativity
  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/cat (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::bob)))]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/cat (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/cat (s/cat (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/cat (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(cat-tests)

(deftest alt-tests

  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 4, 4)
                                   (0, "!(Object,alice,carol)", 1)
                                   (0, "!(Object,alice,bob)", 2)
                                   (1, "?(alice,carol)", 3)
                                   (2, "?(alice,bob)", 3)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))
        lts2 (lts/lts (s/aldebaran des (0, 6, 5)
                                   (0, "!(Object,alice,bob)", 1)
                                   (0, "!(Object,alice,carol)", 2)
                                   (0, "!(Object,alice,dave)", 3)
                                   (1, "?(alice,bob)", 4)
                                   (2, "?(alice,carol)", 4)
                                   (3, "?(alice,dave)", 4)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Idempotence
  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/alt (s/-->> ::alice ::bob)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Commutativity
  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/alt (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::bob)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/alt (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/alt (s/alt (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/alt (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(alt-tests)

(deftest par-tests

  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 12, 9)
                                   (0, "!(Object,alice,bob)", 1)
                                   (0, "!(Object,alice,carol)", 2)
                                   (1, "?(alice,bob)", 3)
                                   (1, "!(Object,alice,carol)", 4)
                                   (2, "!(Object,alice,bob)", 4)
                                   (2, "?(alice,carol)", 8)
                                   (3, "!(Object,alice,carol)", 5)
                                   (4, "?(alice,bob)", 5)
                                   (4, "?(alice,carol)", 7)
                                   (5, "?(alice,carol)", 6)
                                   (7, "?(alice,bob)", 6)
                                   (8, "!(Object,alice,bob)", 7)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))
        lts2 (lts/lts (s/aldebaran des (0, 54, 27)
                                   (0, "!(Object,alice,bob)", 1)
                                   (0, "!(Object,alice,carol)", 2)
                                   (0, "!(Object,alice,dave)", 3)
                                   (1, "?(alice,bob)", 4)
                                   (1, "!(Object,alice,carol)", 5)
                                   (1, "!(Object,alice,dave)", 6)
                                   (2, "!(Object,alice,bob)", 5)
                                   (2, "?(alice,carol)", 21)
                                   (2, "!(Object,alice,dave)", 22)
                                   (3, "!(Object,alice,bob)", 6)
                                   (3, "!(Object,alice,carol)", 22)
                                   (3, "?(alice,dave)", 26)
                                   (4, "!(Object,alice,carol)", 7)
                                   (4, "!(Object,alice,dave)", 8)
                                   (5, "?(alice,bob)", 7)
                                   (5, "?(alice,carol)", 15)
                                   (5, "!(Object,alice,dave)", 16)
                                   (6, "?(alice,bob)", 8)
                                   (6, "!(Object,alice,carol)", 16)
                                   (6, "?(alice,dave)", 20)
                                   (7, "?(alice,carol)", 9)
                                   (7, "!(Object,alice,dave)", 10)
                                   (8, "!(Object,alice,carol)", 10)
                                   (8, "?(alice,dave)", 14)
                                   (9, "!(Object,alice,dave)", 11)
                                   (10, "?(alice,carol)", 11)
                                   (10, "?(alice,dave)", 13)
                                   (11, "?(alice,dave)", 12)
                                   (13, "?(alice,carol)", 12)
                                   (14, "!(Object,alice,carol)", 13)
                                   (15, "?(alice,bob)", 9)
                                   (15, "!(Object,alice,dave)", 17)
                                   (16, "?(alice,bob)", 10)
                                   (16, "?(alice,carol)", 17)
                                   (16, "?(alice,dave)", 19)
                                   (17, "?(alice,bob)", 11)
                                   (17, "?(alice,dave)", 18)
                                   (18, "?(alice,bob)", 12)
                                   (19, "?(alice,bob)", 13)
                                   (19, "?(alice,carol)", 18)
                                   (20, "?(alice,bob)", 14)
                                   (20, "!(Object,alice,carol)", 19)
                                   (21, "!(Object,alice,bob)", 15)
                                   (21, "!(Object,alice,dave)", 23)
                                   (22, "!(Object,alice,bob)", 16)
                                   (22, "?(alice,carol)", 23)
                                   (22, "?(alice,dave)", 25)
                                   (23, "!(Object,alice,bob)", 17)
                                   (23, "?(alice,dave)", 24)
                                   (24, "!(Object,alice,bob)", 18)
                                   (25, "!(Object,alice,bob)", 19)
                                   (25, "?(alice,carol)", 24)
                                   (26, "!(Object,alice,bob)", 20)
                                   (26, "!(Object,alice,carol)", 25)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-idempotence
  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/par (s/-->> ::alice ::bob)))]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Commutativity
  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/par (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::bob)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/par (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/par (s/par (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/par (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)
                             (s/-->> ::alice ::dave)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(par-tests)

(deftest vector-tests
  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)])
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       (s/-->> ::alice ::carol)])
        lts2 (lts/lts (s/aldebaran des (0, 4, 5)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,alice,carol)", 3)
                                   (3, "?(alice,carol)", 4)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       (s/-->> ::alice ::carol)
                       (s/-->> ::alice ::dave)])
        lts2 (lts/lts (s/aldebaran des (0, 6, 7)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,alice,carol)", 3)
                                   (3, "?(alice,carol)", 4)
                                   (4, "!(Object,alice,dave)", 5)
                                   (5, "?(alice,dave)", 6)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-idempotence
  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       (s/-->> ::alice ::bob)])
        lts2 (lts/lts [(s/-->> ::alice ::bob)])]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Non-commutativity
  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       (s/-->> ::alice ::carol)])
        lts2 (lts/lts [(s/-->> ::alice ::carol)
                       (s/-->> ::alice ::bob)])]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Associativity
  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       [(s/-->> ::alice ::carol)
                        (s/-->> ::alice ::dave)]])
        lts2 (lts/lts [[(s/-->> ::alice ::bob)
                        (s/-->> ::alice ::carol)]
                       (s/-->> ::alice ::dave)])]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  ;; Flattening
  (let [lts1 (lts/lts [(s/-->> ::alice ::bob)
                       [(s/-->> ::alice ::carol)
                        (s/-->> ::alice ::dave)]])
        lts2 (lts/lts [(s/-->> ::alice ::bob)
                       (s/-->> ::alice ::carol)
                       (s/-->> ::alice ::dave)])]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(vector-tests)

(deftest multiary-tests
  (let [lts1 (lts/lts (s/cat (s/alt (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/-->> ::alice ::dave)))
        lts2 (lts/lts (s/alt (s/cat (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::dave))
                             (s/cat (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/alt (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::dave))))
        lts2 (lts/lts (s/alt (s/cat (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/cat (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::dave))))]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/par (s/-->> ::alice ::bob)
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/alt (s/cat (s/-->> ::alice ::bob)
                                    (s/-->> ::alice ::carol))
                             (s/cat (s/-->> ::alice ::carol)
                                    (s/-->> ::alice ::bob))))]
    (is (lts/not-bisimilar? lts1 lts2) (msg lts1 lts2))))

(multiary-tests)

;;;;
;;;; Conditional operators
;;;;

(deftest if-tests
  (let [lts1 (lts/lts (s/if true (s/-->> ::alice ::bob)
                                 (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/if false (s/-->> ::alice ::bob)
                                  (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,carol)", 1)
                                   (1, "?(alice,carol)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/if (= (inc 1) 2) (s/-->> ::alice ::bob)
                                          (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(if-tests)

;;;;
;;;; Recursion operators
;;;;

(deftest loop-recur-tests
  (let [lts1 (lts/lts (s/loop swap [r1 ::alice
                                    r2 ::bob]
                              (s/-->> (s/role r1) (s/role r2))
                              (s/-->> (s/role r2) (s/role r1))
                              (s/recur swap r2 r1)))
        lts2 (lts/lts (s/aldebaran des (0, 8, 8)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,bob,alice)", 3)
                                   (3, "?(bob,alice)", 4)
                                   (4, "!(Object,bob,alice)", 5)
                                   (5, "?(bob,alice)", 6)
                                   (6, "!(Object,alice,bob)", 7)
                                   (7, "?(alice,bob)", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop swap [;; Unfortunately, cannot bind to (s/role ::alice) and (s/role ::bob).
                                    ;; See: https://stackoverflow.com/questions/40161751
                                    r1 (discourje.spec/role ::alice)
                                    r2 (discourje.spec/role ::bob)]
                              (s/-->> r1 r2)
                              (s/-->> r2 r1)
                              (s/recur swap r2 r1)))
        lts2 (lts/lts (s/aldebaran des (0, 8, 8)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,bob,alice)", 3)
                                   (3, "?(bob,alice)", 4)
                                   (4, "!(Object,bob,alice)", 5)
                                   (5, "?(bob,alice)", 6)
                                   (6, "!(Object,alice,bob)", 7)
                                   (7, "?(alice,bob)", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop pipe [i 0
                                    n 3]
                              (s/if (< i (dec n))
                                (s/cat (s/-->> (::alice i) (::alice (inc i)))
                                       (s/recur pipe (inc i) n)))))
        lts2 (lts/lts (s/aldebaran des (0, 4, 5)
                                   (0, "!(Object,alice[0],alice[1])", 1)
                                   (1, "?(alice[0],alice[1])", 2)
                                   (2, "!(Object,alice[1],alice[2])", 3)
                                   (3, "?(alice[1],alice[2])", 4)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop ring [r-name ::alice
                                    i 0
                                    n 3]
                              (s/if (< i n)
                                (s/cat (s/-->> (r-name i) (r-name (mod (inc i) n)))
                                       (s/recur ring r-name (inc i) n)))))
        lts2 (lts/lts (s/aldebaran des (0, 6, 7)
                                   (0, "!(Object,alice[0],alice[1])", 1)
                                   (1, "?(alice[0],alice[1])", 2)
                                   (2, "!(Object,alice[1],alice[2])", 3)
                                   (3, "?(alice[1],alice[2])", 4)
                                   (4, "!(Object,alice[2],alice[0])", 5)
                                   (5, "?(alice[2],alice[0])", 6)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop omega []
                              (s/loop ring [r-name ::alice
                                            i 0
                                            n 3]
                                      (s/if (< i n)
                                        (s/cat (s/-->> (r-name i) (r-name (mod (inc i) n)))
                                               (s/recur ring r-name (inc i) n))))
                              (s/recur omega)))
        lts2 (lts/lts (s/aldebaran des (0, 7, 7)
                                   (0, "!(Object,alice[0],alice[1])", 1)
                                   (1, "?(alice[0],alice[1])", 2)
                                   (2, "!(Object,alice[1],alice[2])", 3)
                                   (3, "?(alice[1],alice[2])", 4)
                                   (4, "!(Object,alice[2],alice[0])", 5)
                                   (5, "?(alice[2],alice[0])", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop multicast [i 0
                                         n 3]
                              (s/if (< i n)
                                (s/par (s/-->> ::alice (::bob i))
                                       (s/recur multicast (inc i) n)))))
        lts2 (lts/lts (s/aldebaran des (0, 54, 27)
                                   (0, "!(Object,alice,bob[0])", 1)
                                   (0, "!(Object,alice,bob[1])", 2)
                                   (0, "!(Object,alice,bob[2])", 3)
                                   (1, "?(alice,bob[0])", 4)
                                   (1, "!(Object,alice,bob[1])", 5)
                                   (1, "!(Object,alice,bob[2])", 6)
                                   (2, "!(Object,alice,bob[0])", 5)
                                   (2, "?(alice,bob[1])", 21)
                                   (2, "!(Object,alice,bob[2])", 22)
                                   (3, "!(Object,alice,bob[0])", 6)
                                   (3, "!(Object,alice,bob[1])", 22)
                                   (3, "?(alice,bob[2])", 26)
                                   (4, "!(Object,alice,bob[1])", 7)
                                   (4, "!(Object,alice,bob[2])", 8)
                                   (5, "?(alice,bob[0])", 7)
                                   (5, "?(alice,bob[1])", 15)
                                   (5, "!(Object,alice,bob[2])", 16)
                                   (6, "?(alice,bob[0])", 8)
                                   (6, "!(Object,alice,bob[1])", 16)
                                   (6, "?(alice,bob[2])", 20)
                                   (7, "?(alice,bob[1])", 9)
                                   (7, "!(Object,alice,bob[2])", 10)
                                   (8, "!(Object,alice,bob[1])", 10)
                                   (8, "?(alice,bob[2])", 14)
                                   (9, "!(Object,alice,bob[2])", 11)
                                   (10, "?(alice,bob[1])", 11)
                                   (10, "?(alice,bob[2])", 13)
                                   (11, "?(alice,bob[2])", 12)
                                   (13, "?(alice,bob[1])", 12)
                                   (14, "!(Object,alice,bob[1])", 13)
                                   (15, "?(alice,bob[0])", 9)
                                   (15, "!(Object,alice,bob[2])", 17)
                                   (16, "?(alice,bob[0])", 10)
                                   (16, "?(alice,bob[1])", 17)
                                   (16, "?(alice,bob[2])", 19)
                                   (17, "?(alice,bob[0])", 11)
                                   (17, "?(alice,bob[2])", 18)
                                   (18, "?(alice,bob[0])", 12)
                                   (19, "?(alice,bob[0])", 13)
                                   (19, "?(alice,bob[1])", 18)
                                   (20, "?(alice,bob[0])", 14)
                                   (20, "!(Object,alice,bob[1])", 19)
                                   (21, "!(Object,alice,bob[0])", 15)
                                   (21, "!(Object,alice,bob[2])", 23)
                                   (22, "!(Object,alice,bob[0])", 16)
                                   (22, "?(alice,bob[1])", 23)
                                   (22, "?(alice,bob[2])", 25)
                                   (23, "!(Object,alice,bob[0])", 17)
                                   (23, "?(alice,bob[2])", 24)
                                   (24, "!(Object,alice,bob[0])", 18)
                                   (25, "!(Object,alice,bob[0])", 19)
                                   (25, "?(alice,bob[1])", 24)
                                   (26, "!(Object,alice,bob[0])", 20)
                                   (26, "!(Object,alice,bob[1])", 25)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/loop anycast [i 0
                                       n 3]
                              (s/if (< i (dec n))
                                (s/alt (s/-->> ::alice (::bob i))
                                       (s/recur anycast (inc i) n))
                                (s/-->> ::alice (::bob i)))))
        lts2 (lts/lts (s/aldebaran des (0, 6, 5)
                                   (0, "!(Object,alice,bob[0])", 1)
                                   (0, "!(Object,alice,bob[1])", 2)
                                   (0, "!(Object,alice,bob[2])", 3)
                                   (1, "?(alice,bob[0])", 4)
                                   (2, "?(alice,bob[1])", 4)
                                   (3, "?(alice,bob[2])", 4)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(loop-recur-tests)

;;;;;
;;;;; Regex operators
;;;;;

(deftest ω-tests
  (let [lts1 (lts/lts (s/ω (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 2)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/ω (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/cat (s/ω (s/-->> ::alice ::bob))
                             (s/-->> ::alice ::carol)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/ω (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/ω (s/ω (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/ω (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/omega (s/-->> ::alice ::bob)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(ω-tests)

(deftest *-tests
  (let [lts1 (lts/lts (s/* (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 2)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/cat (s/* (s/-->> ::alice ::bob))
                             (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 4, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 0)
                                   (0, "!(Object,alice,carol)", 2)
                                   (2, "?(alice,carol)", 3)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/* (s/* (s/-->> ::alice ::bob))
                           (s/-->> ::alice ::carol)))
        lts2 (lts/lts (s/aldebaran des (0, 4, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 0)
                                   (0, "!(Object,alice,carol)", 2)
                                   (2, "?(alice,carol)", 0)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/* (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/cat (s/* (s/-->> ::alice ::bob))
                             (s/* (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/* (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/* (s/* (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(*-tests)

(deftest +-tests
  (let [lts1 (lts/lts (s/+ (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 3, 3)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)
                                   (2, "!(Object,alice,bob)", 1)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/+ (s/-->> ::alice ::bob)
                           (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/+ (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/+ (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/cat (s/-->> ::alice ::bob)
                             (s/* (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(+-tests)

(deftest ?-tests
  (let [lts1 (lts/lts (s/? (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/aldebaran des (0, 2, 2)
                                   (0, "!(Object,alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/cat (s/? (s/-->> ::alice ::bob))
                             (s/-->> ::alice ::bob)))
        lts2 (lts/lts (s/alt (s/-->> ::alice ::bob)
                             (s/cat (s/-->> ::alice ::bob) (s/-->> ::alice ::bob))))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(?-tests)

;;;;;
;;;;; Definition operators
;;;;;

(deftest apply-tests
  (let [lts1 (lts/lts (s/apply ::s/-->>not [;; Code needs to be passed quoted
                                            'Long
                                            (discourje.spec/role ::alice)
                                            (discourje.spec/role ::bob)]))
        lts2 (lts/lts (s/aldebaran des (0, 2, 3)
                                   (0, "!((fn [x] (not= (type x) Long)),alice,bob)", 1)
                                   (1, "?(alice,bob)", 2)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/apply ::s/pipe [(discourje.spec/predicate Long) ::alice 10 14]))
        lts2 (lts/lts (s/aldebaran des (0, 6, 7)
                                   (0, "!(Long,alice[10],alice[11])", 1)
                                   (1, "?(alice[10],alice[11])", 2)
                                   (2, "!(Long,alice[11],alice[12])", 3)
                                   (3, "?(alice[11],alice[12])", 4)
                                   (4, "!(Long,alice[12],alice[13])", 5)
                                   (5, "?(alice[12],alice[13])", 6)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2)))

  (let [lts1 (lts/lts (s/apply ::s/pipe [(discourje.spec/predicate Long) ::alice 4]))
        lts2 (lts/lts (s/aldebaran des (0, 6, 7)
                                   (0, "!(Long,alice[0],alice[1])", 1)
                                   (1, "?(alice[0],alice[1])", 2)
                                   (2, "!(Long,alice[1],alice[2])", 3)
                                   (3, "?(alice[1],alice[2])", 4)
                                   (4, "!(Long,alice[2],alice[3])", 5)
                                   (5, "?(alice[2],alice[3])", 6)))]
    (is (lts/bisimilar? lts1 lts2) (msg lts1 lts2))))

(apply-tests)
