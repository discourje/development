(ns discourje.TwoBuyerProtocol
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]))

;For more information about this particular protocol see: https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf
;Define stakeholders of the protocol as channels
(def buyer1 "Define Buyer1 as a channel" (chan))
(def buyer2 "Define Buyer2 as a channel"(chan))
(def seller "Define Seller as a channel"(chan))

;Define method to publish something on a channel
(defn publish [channel] (pub channel :tag))

;Define a method to send a message with a tag on a channel
(defn send-with-tag [msg tag channel]
      (>!! channel {:tag tag
                 :msg (:msg msg)}))

;first subscribe to :title tag on sellers channel, and when received publish quote on the seller channel itself to be catched by buyers
(let [s seller]
  (sub (publish s) :title s)                                ;subscribe to publish on channel s:seller
  (go (send-with-tag (+ (rand-int 30) 1) :quote s)))        ;send a message (random number between 1 and 30) with tag `quote' over channel s(seller)

;subscribe buyer 1 to sellers channel to :quote tag
(let [b1 buyer1]
  (sub (publish seller) :quote seller)                      ;subscribe to channel
  (go (send-with-tag (rand-int (<! seller)) :quoteDiv buyer2))) ;fire quote div (random number in quote value)
