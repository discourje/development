(ns discourje.twoBuyerProtocol.pipes.participants.buyer1
  (:require [discourje.twoBuyerProtocol.pipes.participant :as part :refer :all]
            [discourje.twoBuyerProtocol.pipes.util :as util :refer :all]))

;(def buyer1 (->participant "buyer1"))
(participant "buyer1")

(defn getRandomBook []
  "Get a random book from the book collection in the util"
  (rand-nth bookCollection))

(defn putTitle
  "put a random book title on the target channel"
  [channel]
  (putOnChannel channel (getRandomBook) :title))

;
;(defn putQuoteDiv [channel]
;  (putOnChannel channel ))
