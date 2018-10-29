(ns discourje.twoBuyerProtocol
  (:require [discourje.core :refer :all]
            [clojure.core.async :as async :refer :all]
            [clojure.core :refer :all]))

(def buyer1 (createParticipant))
(def buyer2 (createParticipant))
(def seller (createParticipant))


(defn generateBook []
  (str "TheJoyOfClojure"))

(defn generateQuote
  "generate random integer between 1(inclusive) and 30(inclusive)"
  []
  (fn [title]
    (
      (println (format "received title: %s" title))
      (+ (rand-int 30) 1))))

(defn twoBuyersProtocol
  "This protocol will enforce the correct `conversation' between the participants."
  [b1 b2 s]
  (sendMessage (generateBook) b1 s);send title from buyer1 to seller
  (sendMessage (generateQuote) s b1 b2);send quote from seller to both buyers
  ();send quoteDiv from buyer1 to buyer2
  ;send ok from buyer2 to seller
  ;send address from buyer 2 to seller
  ;send data from seller to buyer2
  ;send quite from buyer2 to seller
  )
(twoBuyersProtocol buyer1 buyer2 seller)