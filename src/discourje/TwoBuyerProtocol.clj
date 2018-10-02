(ns discourje.TwoBuyerProtocol
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]))

;For more information about this particular protocol see: https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf
;Define stakeholders of the protocol as channels
(def buyer1 "Define Buyer1 as a channel" (chan))
(def buyer2 "Define Buyer2 as a channel" (chan))
(def seller "Define Seller as a channel" (chan))

;Define method to publish something on a channel
(defn publish [channel] (pub channel :tag))

;Define a method to send a message with a tag on a channel
(defn send-with-tag [msg tag channel]
      (>!! channel {:tag tag
                 :msg (:msg msg)}))


(send-with-tag "test" :quote buyer1)

;first subscribe to :title tag on sellers channel, and when received, publish quote to the buyers
(let [s seller]
  (sub (publish s) :title s)
  (sub (publish s) :ok s)     ;subscribe to publish on channel s:seller
  (sub (publish s) :address s)     ;subscribe to publish on channel s:seller
  (sub (publish s) :quit s)     ;subscribe to publish on channel s:seller
  (when-let [message (<! s)]
    (cond
      (= message :title)
      (let [quote (+ (rand-int 30) 1)]                          ;use let construct to define a quote variable as a random number between 1 and 30
        (go (send-with-tag quote :quote buyer1))
        (go (send-with-tag quote :quote buyer2)))
      (= message :ok) 1
      (= message :address) (go (send-with-tag "03-10-18" :date buyer2))
      (= message :quit) 3)))        ;send a message (random number between 1 and 30) with tag `quote' over to buyer 1 and 2

;subscribe buyer 1 to :quote tag
(let [b1 buyer1]
  (sub (publish b1) :quote b1)                      ;subscribe to channel
  (go (send-with-tag (rand-int (<! b1)) :quoteDiv buyer2))) ;fire quote div (random number in quote value)

;subscribe buyer 1 to  :quote tag
(let [b2 buyer2]
  (sub (publish b2) :quote b2)
  (sub (publish b2) :quoteDiv b2) ;subscribe to channel on tag quote and quoteDiv
  (sub (publish b2) :date b2) ;subscribe to channel on tag quote and quoteDiv
  (when-let [message (<! b2)]
    (cond
      (= message :quote) 1
      (= message :quoteDiv) 2
      (= message :date) 3))) ;fire quote div (random number in quote value)
