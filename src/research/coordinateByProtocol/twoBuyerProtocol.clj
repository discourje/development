(ns research.coordinateByProtocol.twoBuyerProtocol
  (:require [research.coordinateByProtocol.core :refer :all]
            [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def buyer1 (createParticipant))
(def buyer2 (createParticipant))
(def seller (createParticipant))

(defn generateBook
  "generate simple book title"
  []
  (str "TheJoyOfClojure"))

(defn quoteBook
  "generate random integer between 1(inclusive) and 30(inclusive)"
  [title]
  (println (format "received title: %s" title))
  (let [x (+ (rand-int 30) 1)]
    (println (format "random number is: %s" x))
    x))

(defn generateAddress
  "generates the address"
  []
  "Open University, Valkenburgerweg 177, 6419 AT, Heerlen")

(defn quoteDiv
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "received quote: %s" quote))
  (+ (rand-int quote) 1))

(defn contribute?
  "returns true when the received quote is <= 15 (50% chance at this moment)"
  [quote]
  (println (format "Only contribute up to 15, quote is %s" quote))
  (<= quote 15))

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

(defmacro delaySendInput
  [f from to]
  `'(sendInput ~f ~from ~to))

(defn delayInputs [list]
  (for [tuple list]
    (let [f (nth tuple 0)
          from (nth tuple 1)
          to (nth tuple 2)]
      (println f)
      (println from)
      (println to)
      (delaySendInput `f from to))))

;(eval (eval (macroexpand '(delaySendInput (sendOffData "ok") buyer2 seller))))
;(eval (delaySendInput (sendOffData "ok") buyer2 seller))

(delayInputs (list
               (list (sendOffData "ok") buyer2 seller)
               (list (sendOffFunction generateAddress) buyer2 seller)
               (list (sendOffData (getDate 3)) seller buyer2))
             )

(list
  ;(println ("true branchable taken"))                 ;true branchable
  (delaySendInput (sendOffData "ok") buyer2 seller)         ;send ok from buyer2 to seller
  (delaySendInput (sendOffFunction generateAddress) buyer2 seller) ;send address from buyer 2 to seller
  (delaySendInput (sendOffData (getDate 3)) seller buyer2)  ;send date from seller to buyer2

  )


(defn twoBuyersStakeholdersProtocol
  "This protocol will enforce the correct `conversation' between the participants."
  [b1 b2 s]
  (sendInput (sendOffData (generateBook)) b1 s)             ;send title from buyer1 to seller
  (sendInput (sendOffConsumingInput quoteBook s) s b1 b2)   ;send quote from seller to both buyers, consumes the current value of the inputchannel for Seller
  (sendInput (sendOffConsumingInput quoteDiv b1) b1 b2)     ;send quoteDiv from buyer1 to buyer2
  (choice b2 contribute?                                    ;decide to contribute to continue or not (taking true or false branchable)
          (list
            (delaySendInput (sendOffData "ok") buyer2 seller) ;send ok from buyer2 to seller
            (delaySendInput (sendOffFunction generateAddress) buyer2 seller) ;send address from buyer 2 to seller
            (delaySendInput (sendOffData (getDate 3)) seller buyer2) ;send date from seller to buyer2

            )
          (list
            (delaySendInput (sendOffData "quit") buyer2 seller) ;send quite from buyer2 to seller
            )
          "test"
          )
  )

(twoBuyersStakeholdersProtocol buyer1 buyer2 seller)
