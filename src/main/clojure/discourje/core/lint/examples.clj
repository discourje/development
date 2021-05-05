(ns discourje.core.lint.examples
  (:require [discourje.core.async :as a]
            [discourje.core.spec :as s]
            [discourje.core.lint :as c])
  (:import (java.util Date)))

;;;;;
;;;;; Specification
;;;;;

(s/defroles ::buyer1 ::buyer2 ::seller)

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/alt (s/--> #{true} ::buyer2 ::seller)
                (s/--> (fn [v] (= v false)) ::buyer2 ::seller))
         (s/par (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller)
                (s/close ::buyer2 ::buyer1)
                (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Implementation
;;;;;

(defn main []
  (let [;; Monitor
        monitor (a/monitor (s/session ::two-buyer []))

        ;; Channels
        b1->b2 (a/chan buyer1 buyer2 monitor {})
        b1->s (a/chan buyer1 seller monitor {})
        b2->b1 (a/chan buyer2 buyer1 monitor {})
        b2->s (a/chan buyer2 seller monitor {})
        s->b1 (a/chan seller buyer1 monitor {})
        s->b2 (a/chan seller buyer2 monitor {})]

    ;; Buyer1
    (a/thread (a/>!! b1->s "...")                           ;; Send title to Seller
              (let [quote (a/<!! s->b1)]                    ;; Receive quote from Seller
                (a/>!! b1->b2 (/ quote 2)))                 ;; Send share to Buyer2
              (a/close! b1->b2)                             ;; Close channel to Buyer2
              (a/close! b1->s))                             ;; Close channel to Seller

    ;; Buyer2
    (a/thread (let [quote (a/<!! s->b2)                     ;; Receive quote from Seller
                    share (a/<!! b1->b2)]                   ;; Receive share from Buyer1
                (if (< (* share 2) quote)
                  (do (a/>!! b2->s true))                   ;; - Send internal choice (true) to Seller
                  (do (a/>!! b2->s false))))                ;; - Send internal choice (false) to Seller
              (a/close! b2->b1)                             ;; Close channel to Buyer1
              (a/close! b2->s))                             ;; Close channel to Seller

    ;; Seller
    (a/thread (a/<!! b1->s)                                 ;; Receive title from Seller
              (a/>!! s->b1 (int 20))                        ;; Send quote to Buyer1
              (a/>!! s->b2 (int 20))                        ;; Send quote to Buyer2
              (println (a/<!! b2->s))                       ;; Receive external choice from Buyer2
              (a/close! s->b1)                              ;; Close channel to Buyer1
              (a/close! s->b2))                             ;; Close channel to Buyer2
    nil))

;;;;;
;;;;; Run
;;;;;

(main)

;;;;;
;;;;; Implementation
;;;;;

(defn main []
  (let [;; Monitor
        monitor (a/monitor (s/session ::two-buyer []))

        ;; Channels
        b1->b2 (a/chan buyer1 buyer2 monitor {})
        b1->s (a/chan buyer1 seller monitor {})
        b2->b1 (a/chan buyer2 buyer1 monitor {})
        b2->s (a/chan buyer2 seller monitor {})
        s->b1 (a/chan seller buyer1 monitor {})
        s->b2 (a/chan seller buyer2 monitor {})]

    ;; Buyer1
    (a/thread (a/>!! b1->s "...")                           ;; Send title to Seller
              (let [quote (a/<!! s->b1)]                    ;; Receive quote from Seller
                (a/>!! b1->b2 (int (/ quote 2))))           ;; Send share to Buyer2
              (a/close! b1->b2)                             ;; Close channel to Buyer2
              (a/close! b1->s))                             ;; Close channel to Seller

    ;; Buyer2
    (a/thread (let [quote (a/<!! s->b2)                     ;; Receive quote from Seller
                    share (a/<!! b1->b2)]                   ;; Receive share from Buyer1
                (if (< (* share 2) quote)
                  (do (a/>!! b2->s true))                   ;; - Send internal choice (true) to Seller
                  (do (a/>!! b2->s false))))                ;; - Send internal choice (false) to Seller
              (a/close! b2->b1)                             ;; Close channel to Buyer1
              (a/close! b2->s))                             ;; Close channel to Seller

    ;; Seller
    (a/thread (a/<!! b1->s)                                 ;; Receive title from Seller
              (a/>!! s->b1 (int 20))                        ;; Send quote to Buyer1
              (a/>!! s->b2 (int 20))                        ;; Send quote to Buyer2
              (println (a/<!! b2->s))                       ;; Receive external choice from Buyer2
              (a/close! s->b1)                              ;; Close channel to Buyer1
              (a/close! s->b2))                             ;; Close channel to Buyer2
    nil))

;;;;;
;;;;; Run
;;;;;

(main)

;;;;;
;;;;; Lint
;;;;;

(c/lint (s/session ::two-buyer []))

;;;;;
;;;;; Lint
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/alt (s/--> (fn [v] (= v true)) ::buyer2 ::seller)
                (s/--> (fn [v] (= v false)) ::buyer2 ::seller))
         (s/par (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller)
                (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Lint
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/par (s/alt (s/--> (fn [v] (= v true)) ::buyer2 ::seller)
                       (s/--> (fn [v] (= v false)) ::buyer2 ::seller))
                (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller))
         (s/par (s/close ::buyer2 ::seller)
                (s/close ::seller ::buyer1)
                (s/close ::seller ::buyer2))))

;;;;;
;;;;; Lint
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Specification
;;;;;

(s/defsession ::two-buyer []
  (s/cat (s/--> String ::buyer1 ::seller)
         (s/--> Integer ::seller ::buyer1)
         (s/--> Integer ::seller ::buyer2)
         (s/--> Integer ::buyer1 ::buyer2)
         (s/par (s/cat (s/alt (s/--> (fn [v] (= v true)) ::buyer2 ::seller)
                              (s/--> (fn [v] (= v false)) ::buyer2 ::seller))
                       (s/par (s/close ::buyer2 ::seller)
                              (s/close ::seller ::buyer1)
                              (s/close ::seller ::buyer2)))
                (s/close ::buyer1 ::buyer2)
                (s/close ::buyer1 ::seller))))

;;;;;
;;;;; Lint
;;;;;

(c/lint (s/session ::two-buyer [])
        :exclude #{:cant-terminate})

;;;;;
;;;;; Run
;;;;;

(main)

;;;;;
;;;;; Implementation
;;;;;

(defn main []
  (let [;; Monitor
        monitor (a/monitor (s/session ::two-buyer []))

        ;; Channels
        b1->b2 (a/chan buyer1 buyer2 monitor {})
        b1->s (a/chan buyer1 seller monitor {})
        ;; b2->b1 (a/chan buyer2 buyer1 monitor {}) ------- ;; REMOVED
        b2->s (a/chan buyer2 seller monitor {})
        s->b1 (a/chan seller buyer1 monitor {})
        s->b2 (a/chan seller buyer2 monitor {})]

    ;; Buyer1
    (a/thread (a/>!! b1->s "...")                           ;; Send title to Seller
              (let [quote (a/<!! s->b1)]                    ;; Receive quote from Seller
                (a/>!! b1->b2 (int (/ quote 2))))           ;; Send share to Buyer2
              (a/close! b1->b2)                             ;; Close channel to Buyer2
              (a/close! b1->s))                             ;; Close channel to Seller

    ;; Buyer2
    (a/thread (let [quote (a/<!! s->b2)                     ;; Receive quote from Seller
                    share (a/<!! b1->b2)]                   ;; Receive share from Buyer1
                (if (< (* share 2) quote)
                  (do (a/>!! b2->s true)                    ;; - Send internal choice (true) to Seller
                      (a/>!! b2->s "...")                   ;;   Send address to Seller
                      (a/<!! s->b2))                        ;;   Receive date from Seller
                  (do (a/>!! b2->s false))))                ;; - Send internal choice (false) to Seller

              ;; (a/close! b2->b1) ------------------------ ;; REMOVED
              (a/close! b2->s))                             ;; Close channel to Seller

    ;; Seller
    (a/thread (a/<!! b1->s)                                 ;; Receive title from Seller
              (a/>!! s->b1 (int 20))                        ;; Send quote to Buyer1
              (a/>!! s->b2 (int 20))                        ;; Send quote to Buyer2
              (if (a/<!! b2->s)                             ;; Receive external choice from Buyer2
                (do (a/<!! b2->s)                           ;; - Receive address from Buyer2
                    (a/>!! s->b2 (Date.)))                  ;;   Send date to Buyer2
                (do nil))                                   ;; - (Skip)

              (a/close! s->b1)                              ;; Close channel to Buyer1
              (a/close! s->b2))                             ;; Close channel to Buyer2
    nil))

;;;;;
;;;;; Run
;;;;;

(main)