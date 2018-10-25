(ns discourje.twoBuyerProtocol
  (:require [discourje.core :as async :refer :all]
            [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def buyer1 (createParticipant))
(def buyer2 (createParticipant))
(def seller (createParticipant))

(defn twoBuyersProtocol
  "This protocol will enforce the correct `conversation' between the participants."
  [b1 b2 s]
  )

(twoBuyersProtocol buyer1 buyer2 seller)