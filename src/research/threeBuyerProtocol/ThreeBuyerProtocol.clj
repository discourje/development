(ns research.threeBuyerProtocol.ThreeBuyerProtocol
  (:require [discourje.experiments.abstractionExperiments :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.experiments.abstractionExperiments message participant)))

;sample messages
(def helloWorldMessage (message. "Hello World"))
(def helloWorldNumber (message. 1234))
; actors in the protocol: alice, bob , carol, seller,
(def alice (participant. "alice"))
(def bob (participant. "bob"))
(def carol (participant. "carol"))
(def seller (participant. "seller"))

