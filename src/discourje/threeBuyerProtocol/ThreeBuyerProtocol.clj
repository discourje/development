(ns discourje.threeBuyerProtocol.ThreeBuyerProtocol
  (:require [discourje.core :refer :all])
  (:refer-clojure :exclude [send])
  (:import (discourje.core message participant)))

;sample messages
(def helloWorldMessage (message. "Hello World"))
(def helloWorldNumber (message. 1234))
; actors in the protocol: alice, bob , carol, seller,
(def alice (participant. "alice"))
(def bob (participant. "bob"))
(def carol (participant. "carol"))
(def seller (participant. "seller"))

