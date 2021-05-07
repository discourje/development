(ns discourje.core.lint.demonstration
  (:require [discourje.core.async :as a]
            [discourje.core.spec :as s]
            [discourje.core.lint :as c])
  (:import (java.util Date)))

;;;;;
;;;;; Write specification
;;;;;

(s/defroles ::buyer1 ::buyer2 ::seller)

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/--> Boolean ::buyer2 ::seller)
         (s/par (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller)
                (s/close ::buyer2 ::buyer1)
                (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Write implementation
;;;;;

(defn main []
  (let [;; Monitor
        monitor (a/monitor (s/session ::two-buyer []))

        ;; Channels
        c1 (a/chan buyer1 buyer2 monitor {})
        c2 (a/chan buyer1 seller monitor {})
        c3 (a/chan buyer2 buyer1 monitor {})
        c4 (a/chan buyer2 seller monitor {})
        c5 (a/chan seller buyer1 monitor {})
        c6 (a/chan seller buyer2 monitor {})]

    ;; Buyer1
    (a/thread (a/>!! c2 "book")                             ;; Send book to Seller
              (let [x (a/<!! c5)]                           ;; Receive quote from Seller
                (a/>!! c1 (int (/ x 2))))                   ;; Send contribution to Buyer2
              (a/close! c1)                                 ;; Close channel to Buyer2
              (a/close! c2))                                ;; Close channel to Seller

    ;; Buyer2
    (a/thread (let [x (a/<!! c6)                            ;; Receive quote from Seller
                    y (a/<!! c1)                            ;; Receive share from Buyer1
                    z (= x y)]                              ;; Compute decision
                (a/>!! c4 z)                                ;; Send decision to Seller
                (a/close! c3)                               ;; Close channel to Buyer1
                (a/close! c4)))                             ;; Close channel to Seller

    ;; Seller
    (a/thread (a/<!! c2)                                    ;; Receive title from Seller
              (a/>!! c5 (int 20))                           ;; Send quote to Buyer1
              (a/>!! c6 (int 20))                           ;; Send quote to Buyer2
              (println (a/<!! c4))                          ;; Receive decision from Buyer2
              (a/close! c5)                                 ;; Close channel to Buyer1
              (a/close! c6))                                ;; Close channel to Buyer2
    nil))

;;;;;
;;;;; Run implementation with specification
;;;;;

(main)

;;;;;
;;;;; Check specification
;;;;;

(c/lint (s/session ::two-buyer []))

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Fix specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/--> Boolean ::buyer2 ::seller)
         (s/par (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller)
                ;(s/close ::buyer2 ::buyer1) <----- REMOVED
                (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Check specification
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Fix specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/par (s/--> Boolean ::buyer2 ::seller)
                (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller))
         (s/par (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Check specification
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Fix specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/par (s/cat (s/--> Boolean ::buyer2 ::seller)
                       (s/par (s/close ::buyer2 ::seller)
                              (s/close ::seller ::buyer1)
                              (s/close ::seller ::buyer2)))
                (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller))))

;;;;;
;;;;; Check specification
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Run implementation with specification
;;;;;

(main)

;;;;;
;;;;; Fix Implementation
;;;;;

(defn main []
  (let [;; Monitor
        monitor (a/monitor (s/session ::two-buyer []))

        ;; Channels
        c1 (a/chan buyer1 buyer2 monitor {})
        c2 (a/chan buyer1 seller monitor {})
        ;c3 (a/chan buyer2 buyer1 monitor {}) <----- REMOVED
        c4 (a/chan buyer2 seller monitor {})
        c5 (a/chan seller buyer1 monitor {})
        c6 (a/chan seller buyer2 monitor {})]

    ;; Buyer1
    (a/thread (a/>!! c2 "book")                             ;; Send book to Seller
              (let [x (a/<!! c5)]                           ;; Receive quote from Seller
                (a/>!! c1 (int (/ x 2))))                   ;; Send contribution to Buyer2
              (a/close! c1)                                 ;; Close channel to Buyer2
              (a/close! c2))                                ;; Close channel to Seller

    ;; Buyer2
    (a/thread (let [x (a/<!! c6)                            ;; Receive quote from Seller
                    y (a/<!! c1)                            ;; Receive share from Buyer1
                    z (= x y)]                              ;; Compute decision
                (a/>!! c4 z)                                ;; Send decision to Seller
                ;(a/close! c3) <----- REMOVED               ;; Close channel to Buyer1
                (a/close! c4)))                             ;; Close channel to Seller

    ;; Seller
    (a/thread (a/<!! c2)                                    ;; Receive title from Seller
              (a/>!! c5 (int 20))                           ;; Send quote to Buyer1
              (a/>!! c6 (int 20))                           ;; Send quote to Buyer2
              (println (a/<!! c4))                          ;; Receive decision from Buyer2
              (a/close! c5)                                 ;; Close channel to Buyer1
              (a/close! c6))                                ;; Close channel to Buyer2
    nil))

;;;;;
;;;;; Run implementation with specification
;;;;;

(main)