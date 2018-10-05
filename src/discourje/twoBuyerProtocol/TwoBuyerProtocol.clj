(ns discourje.twoBuyerProtocol.TwoBuyerProtocol
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]
            [discourje.twoBuyerProtocol.utilities :as util :refer :all]))

;For more information about this particular protocol see: https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf

;Define stakeholders of the protocol as channels
(def buyer1 "Define Buyer1 as a channel" (chan))
(def buyer2 "Define Buyer2 as a channel" (chan))
(def seller "Define Seller as a channel" (chan))

;subscribe buyer 1 to :quote tag
(let [b1 buyer1]
  (sub (publish b1) :quote b1)
  (go (loop []
    (let [message (<! b1)]
        (when (number? (:msg message))
          (                                                 ;(println "Yes, :quote message content is a number!")
            (send-with-tag (rand-int (:msg message)) :quoteDiv buyer2)
            (print "blabla")))))))

;first subscribe to :title tag on sellers channel, and when received, publish quote to the buyers
(let [s seller]
  (sub (publish s) :title s)
  (sub (publish s) :ok s)
  (sub (publish s) :address s)
  (sub (publish s) :quit s)
  (go
    (loop []
    (when-let [message (<! s)]
    (cond
      (= (:tag message) :title)
      ((println (format "Book title received: %s" (:msg message)))
        (let [quote (+ (rand-int 30) 1)]
          (go (send-with-tag quote :quote buyer1))
          (go (send-with-tag quote :quote buyer2))))
      (= (:tag message) :ok) (println "Ok confirmation received!")
      (= (:tag message) :address) (go (send-with-tag (getRandomDate 5) :date buyer2))
      (= (:tag message) :quit) (closeN! buyer1 buyer2 s))))))

;subscribe buyer 2 to  :quote tag
(let [b2 buyer2]
  (sub (publish b2) :quote b2)
  (sub (publish b2) :quoteDiv b2)
  (sub (publish b2) :date b2)
  (go
    (loop []
    (when-let [message (<! b2)]
        (cond
          (= (:tag message) :quote) (print "message with :quote received, not cached at this moment!")
          (= (:tag message) :quoteDiv) (go (when (number? (:msg message))
                                             (print "Yes, :quoteDiv message content is a number! Sending random decline or accept and address if accepted")
                                             (let [choice randomBoolean]
                                               (if true? choice
                                                         (send-with-tag "Open University, Valkenburgerweg 177, 6419 AT, Heerlen" :address seller))
                                               (send-with-tag choice :ok seller))))
          (= (:tag message) :date) ((print (format "Date received: %s, now quitting" (:msg message)))
                                     (send-with-tag "quit" :quit seller)))))))



(send-with-tag 30 :quote buyer1)
(send-with-tag "no integer" :quote buyer1)
(send-with-tag "not received" :unusedTag buyer1)
(send-with-tag "The Joy of Clojure" :title seller)
