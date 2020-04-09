(ns discourje.core.async-tests
  (:require [clojure.test :refer :all]
            [discourje.core.async :as a]
            [discourje.core.async.spec :as s]))

(defn defroles [f]
  (s/defrole ::alice "alice")
  (s/defrole ::bob "bob")
  (s/defrole ::carol "carol")
  (s/defrole ::dave "dave")
  (f))

(defroles (fn [] true))

(use-fixtures :once defroles)

(deftest put!-get!-tests
  (let [m (s/monitor (s/--> ::alice ::bob))
        c (a/chan (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (is true))

  (let [m (s/monitor (s/loop :* [] (s/--> ::alice ::bob) (s/recur :*)))
        c (a/chan (s/role ::alice) (s/role ::bob) m)]
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (a/put! c 0)
    (a/take! c identity)
    (is true)))
