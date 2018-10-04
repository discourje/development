(ns discourje.TwoBuyerProtocol
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer :all]))

;For more information about this particular protocol see: https://www.doc.ic.ac.uk/~yoshida/multiparty/multiparty.pdf
;Define stakeholders of the protocol as channels
(def buyer1 "Define Buyer1 as a channel" (chan))
(def buyer2 "Define Buyer2 as a channel" (chan))
(def seller "Define Seller as a channel" (chan))

;Define method to assign to publish on a channel
(defn publish "Define method to assign to publish on a channel"
  [channel] (pub channel :tag))

;Define a method to send a message with a tag on a channel
(defn send-with-tag "Send a message with a certain tag on a channel"
  [msg tag channel]
    (>!! channel {:msg msg :tag tag}))

; Generate a new Java Date
(defn getDate "Generate a new date and increment an amount of days"
  [days]
  (let [cal (java.util.Calendar/getInstance)
        d (new java.util.Date)]
    (doto cal
      (.setTime d)
      (.add java.util.Calendar/DATE days)
      (.getTime))))

; Generate a new java date with a random amount of days incremented up to a specified range
(defn getRandomDate "Get a random date, in the future, up to a maximum range (inclusive)"
  [maxRange]
  (getDate (+ (rand-int maxRange) 1)))

(send-with-tag 30 :quote buyer1)
(send-with-tag "no integer" :quote buyer1)
(send-with-tag "not received" :unusedTag buyer1)

;first subscribe to :title tag on sellers channel, and when received, publish quote to the buyers
(let [s seller]
  (sub (publish s) :title s)
  (sub (publish s) :ok s)     ;subscribe to publish on channel s:seller
  (sub (publish s) :address s)     ;subscribe to publish on channel s:seller
  (sub (publish s) :quit s)     ;subscribe to publish on channel s:seller
  (when-let [message (<! s)]
    (cond
      (= (:tag message ) :title)
      (let [quote (+ (rand-int 30) 1)]                          ;use let construct to define a quote variable as a random number between 1 (inclusive) and 30 (inclusive)
        (go (send-with-tag quote :quote buyer1))
        (go (send-with-tag quote :quote buyer2)))
      (= (:tag message ) :ok) (println "Ok confirmation received!")
      (= (:tag message ) :address) (go (send-with-tag (getRandomDate 5)  :date buyer2)) ;send a random date between 1 (inclusive) and 5 (inclusive)
      (= (:tag message ) :quit) 3)))        ;send a message (random number between 1 and 30) with tag `quote' over to buyer 1 and 2

;subscribe buyer 1 to :quote tag
(let [b1 buyer1]
  (sub (publish b1) :quote b1)                              ;subscribe to channel b1 for tag :quote
  (go (let [content (<! b1)]                                ;when a message with tag quote comes in, do a nonblocking take and set it as argument in let block
        (if (number? (:msg content))                        ;make sure the message content is a number
          ((print "Yes, message content is a number!")      ;print message (debugging), not here for practical reasons
           (send-with-tag (rand-int (:msg content)) :quoteDiv buyer2)))))) ;send a new message to buyer2, with a random number attaching tag quoteDiv

;subscribe buyer 2 to  :quote tag
(let [b2 buyer2]
  (sub (publish b2) :quote b2)
  (sub (publish b2) :quoteDiv b2) ;subscribe to channel on tag quote and quoteDiv
  (sub (publish b2) :date b2) ;subscribe to channel on tag quote and quoteDiv
  (when-let [message (<! b2)]
    (cond
      (= (:tag message ) :quote) 1
      (= (:tag message ) :quoteDiv) 2
      (= (:tag message ) :date) 3))) ;fire quote div (random number in quote value)
