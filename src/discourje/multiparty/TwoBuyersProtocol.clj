(ns discourje.multiparty.TwoBuyersProtocol
  (:require [discourje.multiparty.core :refer :all]
            [discourje.multiparty.Buyer1 :refer :all]
            [discourje.multiparty.Buyer2 :refer :all]
            [discourje.multiparty.Seller :refer :all]))

(def channels (generateChannels ["buyer1" "buyer2" "seller"]))

;define a monitor to check communication, this will be used to verify correct conversation
(defrecord monitor [action from to])
(defn choice [trueChannel falseChannel trueBranch falseBranch]
  ())


(defn- defineProtocol[]
  (->monitor "title" "buyer1" "seller")
  (->monitor "quote" "seller" ["buyer1" "buyer2"])
  (->monitor "quoteDiv" "buyer1" "buyer2")
  (choice (->monitor "ok" "buyer2" "seller") (->monitor "quit" "buyer2" "seller")
          (
            (->monitor "address" "buyer2" "seller")
            (->monitor "date" "seller" "buyer2"))
          (
            (->monitor "quit" "buyer2" "seller"))))


(def protocol (defineProtocol))
(def activeMonitor)
(defn setActiveMonitor [x](nth protocol x))
(setActiveMonitor 0)
(defn allowSend [value to]
  )


(defn validCommunication?
  "Checks if communication is valid by comparing input to the active monitor"
  [action from to]
  (and
    (= action (:action activeMonitor))
    (= from (:from activeMonitor))
    (= to (:to activeMonitor))))

(defn communicate [action value from to]
  (if (nil? activeMonitor)
    (println ("protocol does not have a defined channel to monitor!"))
    (if (validCommunication? action from to)
      (allowSend value to)
      (println ("protocol does not allow diverging from communication channels!")))))

