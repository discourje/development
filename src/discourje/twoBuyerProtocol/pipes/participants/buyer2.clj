(ns discourje.twoBuyerProtocol.pipes.participants.buyer2
  (:require [discourje.twoBuyerProtocol.pipes.participant :as part :refer :all]))

(def buyer2 (->participant "buyer2"))

(defn generateAddress []
  {:name "Open University"
   :street "Valkenburgerweg"
   :number 177
   :zipcode "6419 AT"
   :city "Heerlen"})