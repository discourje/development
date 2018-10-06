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
    (when-let [message (<! b1)]
      (logMessage message)
        (when (number? (:msg message))
          (logMessage "yes is number")
          (go-with-tag (rand-int (:msg message)) :quoteDiv buyer2))
      (recur)))))

;first subscribe to :title tag on sellers channel, and when received, publish quote to the buyers
(let [s seller]
  (sub (publish s) :title s)
  (sub (publish s) :ok s)
  (sub (publish s) :address s)
  (sub (publish s) :quit s)
  (go (loop []
    (when-let [message (<! s)]
      (cond
        (= (:tag message) :title)
        (when (string? (:msg message))
          (logMessage (format "Book title received: %s" (:msg message)))
          ;(go-with-tag (+ (rand-int 30) 1) :quote buyer1)
          (let [quote (+ (rand-int 30) 1)]
            (go-with-tag quote :quote buyer1)
            (go-with-tag quote :quote buyer2)))
          ;(let [quote (+ (rand-int 30) 1)]
          ;(logMessage (format "Generated quote is: %s" quote))
          ; (for [args [[quote :quote buyer1] [quote :quote buyer2]]]
          ; (apply go-with-tag args)))))
        (= (:tag message) :ok) (logMessage "Ok confirmation received!")
      (= (:tag message) :address) ((logMessage "Address received!")
                                    (go-with-tag (getRandomDate 5) :date buyer2))
      (= (:tag message) :quit) (closeN! buyer1 buyer2 s))
    (recur)))))

;subscribe buyer 2 to  :quote tag
(let [b2 buyer2]
  (sub (publish b2) :quote b2)
  (sub (publish b2) :quoteDiv b2)
  (sub (publish b2) :date b2)
  (go (loop []
    (when-let [message (<! b2)]
        (cond
          (= (:tag message) :quote) (logMessage "message with :quote received, not cached at this moment!")
          (= (:tag message) :quoteDiv) (go (when (number? (:msg message))
                                             (logMessage "Yes, :quoteDiv message content is a number! Sending random decline or accept and address if accepted")
                                             (let [choice (randomBoolean)]
                                               (logMessage (format "random boolean is: %s" choice))
                                               (go-with-tag choice :ok seller)
                                               (if true? choice
                                                         (go-with-tag "Open University, Valkenburgerweg 177, 6419 AT, Heerlen" :address seller)))))
          (= (:tag message) :date) ((logMessage (format "Date received: %s, now quitting" (:msg message)))
                                     (go-with-tag "quit" :quit seller)))
        (recur)))))



(send-with-tag 30 :quote buyer1)
(send-with-tag "no integer" :quote buyer1)
(send-with-tag "not received" :unusedTag buyer1)
(send-with-tag "The Joy of Clojure" :title seller)

(go-with-tag "The Joy of Clojure" :title seller)
(go-with-tag 30 :quote buyer1)