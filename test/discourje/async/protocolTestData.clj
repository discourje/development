(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest interactableTest
  (let [inter (-->> "1" "A" "B")]
    (is (= "1" (get-action inter)))
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleParallelProtocol []
  (create-protocol [(-->> "1" "A" ["B" "C"])]))

(defn testDualProtocol []
  (create-protocol [(-->> "1" "A" "B")
                    (-->> "2" "B" "A")]))

(defn test-typed-DualProtocol []
  (create-protocol [(-->> java.lang.String "A" "B")
                    (-->> java.lang.String "B" "A")]))

(defn testTripleProtocol []
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")]))

(defn testParallelProtocol []
  (create-protocol [
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

(defn testQuadProtocol []
  (create-protocol [
                    (-->> "start" "main" ["A" "B" "C"])
                    (-->> "1" "A" "B")
                    (-->> "2" "B" "A")
                    (-->> "3" "A" "C")
                    (-->> "4" "C" ["A" "B"])]))

(defn single-choice-protocol []
  (create-protocol [(make-choice [
                                  [(-->> "1" "A" "B")]
                                  [(-->> "hi" "A" "C")]]
                                 )]))

(defn single-choice-in-middle-protocol []
  (create-protocol [(-->> "99" "Start" "Finish")
                    (make-choice [
                                  [(-->> "1" "A" "B")
                                   (-->> "bla" "B" "A")]
                                  [(-->> "2" "A" "C")
                                   (-->> "hello" "C" "A")]]
                                 )
                    (-->> "88" "Finish" "Start")]))

(defn single-choice-5branches-protocol []
  (create-protocol [(make-choice [
                                  [(-->> "1" "A" "B")]
                                  [(-->> "1" "A" "C")]
                                  [(-->> "1" "A" "D")]
                                  [(-->> "1" "A" "E")]
                                  [(-->> "1" "A" "F")]
                                  ]
                                 )
                    (-->> "Done" "A" "End")]))

(defn dual-choice-protocol []
  (create-protocol [(make-choice [
                                  [(-->> "1" "A" "B")]
                                  [(-->> "hi" "A" "C")
                                   (make-choice [
                                                 [(-->> "hiA" "C" "A")]
                                                 [(-->> "hiD" "C" "D")]]
                                                )]]
                                 )
                    (-->> "Done" "A" "End")]))

(defn single-choice-multiple-interactions-protocol []
  (create-protocol [(-->> "1" "A" "B")
                    (-->> "1" "B" "A")
                    (make-choice [
                                  [(-->> "2" "A" "C")
                                   (-->> "2" "C" "A")
                                   (-->> "3" "A" "C")
                                   (-->> "3" "C" "A")]
                                  [(-->> "2" "A" "B")
                                   (-->> "2" "B" "A")
                                   (-->> "3" "A" "B")
                                   (-->> "3" "B" "A")]])
                    (-->> "4" "A" "D")
                    (-->> "4" "D" "A")
                    (-->> "5" "A" ["B" "C" "D"])
                    ]))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(make-choice [
                                  [(-->> "1" "A" "B")]
                                  [(make-choice [
                                                 [(-->> "1" "A" "C")]
                                                 [(-->> "1" "A" "D")]]
                                                )]]
                                 )
                    (-->> "Done" "A" "End")]))

(defn multiple-nested-choice-branch-protocol []
  (create-protocol [(make-choice [;i0
                                  [(make-choice [;i0b00
                                                 [(-->> "1" "A" "B")] ;i0b00b00
                                                 [(-->> "2" "A" "B")]] ;i0b00b10
                                                )]
                                  [(make-choice [;i0b10
                                                 [(-->> "3" "A" "B")] ;i0b10b00
                                                 [(-->> "4" "A" "B")]] ;i0b10b10
                                                )]]
                                 )]))

(defn multiple-nested-branches-protocol []
  (create-protocol [
                    (make-choice [;i0
                                  [(make-choice [;i0b0
                                                 [(-->> "1" "A" "B") ;i0b0b00
                                                  (-->> "2" "B" "A")] ;i0b0b01
                                                 [(-->> "1" "A" "C")]] ;i0b0b10
                                                )]
                                  [(make-choice [;i0b1
                                                 [(make-choice [;i0b1b0
                                                                [(make-choice [;i0b1b0b0
                                                                               [(-->> "1" "A" "D")] ;i0b1b0b0b00
                                                                               [(-->> "1" "A" ["E" "F" "G"]) ;i0b1b0b0b10
                                                                                (-->> "3" "F" "A") ;i0b1b0b0b11
                                                                                (-->> "4" "G" "A")]] ;i0b1b0b0b12
                                                                              )]
                                                                [(-->> "1" "A" "H")]] ;i0b1b0b10
                                                               )]
                                                 [(-->> "1" "A" "I")]] ;i0b1b11
                                                )]]
                                 )
                    (-->> "Done" "A" "End")]                ;i1
                   ))

(defn single-recur-protocol []
  (create-protocol [(-->> "1" "A" "B")                      ;i0
                    (make-recursion :test [;i1
                                           (-->> "1" "B" "A") ; i1r0
                                           (make-choice [;i1r1
                                                         [(-->> "2" "A" "C") ;i1r1b00
                                                          (-->> "2" "C" "A") ;i1r1b01
                                                          (do-recur :test)] ;i1r1b02
                                                         [(-->> "3" "A" "B") ;i1r1b10
                                                          (end-recur :test) ;i1r1b11
                                                          ]
                                                         ])
                                           ])
                    (-->> "end" "A" ["B" "C"])              ; i2
                    ]))

(defn one-recur-with-choice-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-choice [;i0r0
                                                         [(-->> "2" "A" "C") ;i0r0b00
                                                          (do-recur :test)] ;i0r0b01
                                                         [(-->> "3" "A" "B") ;i0r0b10
                                                          (end-recur :test) ;i0r0b11
                                                          ]
                                                         ])
                                           ])
                    ])
  )
(defn one-recur-with-startchoice-and-endchoice-protocol []
  (create-protocol [(make-choice [;i0
                                  [(make-recursion :test [;i0b0r0
                                                          (make-choice [;i0b0r00
                                                                        [(-->> "2" "A" "C") ;i0b0r00b00
                                                                         (do-recur :test)] ;i0b0r00b01
                                                                        [(-->> "3" "A" "B") ;i0b0r00b10
                                                                         (end-recur :test) ;i0b0r00b11
                                                                         ]
                                                                        ])
                                                          ])
                                   ]
                                  [(-->> "2" "A" "C")]      ;i0b10
                                  ])
                    ]))


(defn nested-recur-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-recursion :nested [; i0r0
                                                                    (-->> "1" "B" "A") ;i0r0i0
                                                                    (make-choice [;i0r0i1
                                                                                  [(-->> "2" "A" "C") ;i0r0i1b00
                                                                                   (-->> "2" "C" "A") ;i0r0i1b01
                                                                                   (do-recur :nested)] ;i0r0i1b02
                                                                                  [(-->> "3" "A" "B") ;i0r0i1b10
                                                                                   (end-recur :nested)] ;i0r0i1b11
                                                                                  ])
                                                                    (make-choice [;i0r0i2
                                                                                  [(-->> "2" "A" "C") ;i0r0i2b00
                                                                                   (-->> "2" "C" "D") ;i0r0i2b01
                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                  [(-->> "3" "A" "E") ; i0r0i2b10
                                                                                   (end-recur :test)] ;i0r0i2b11
                                                                                  ])
                                                                    ])]

                                    )
                    (-->> "end" "A" ["B" "C"])              ;i1
                    ]))

(defn multiple-nested-recur-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-recursion :nested [; i0r0
                                                                    (make-choice [;i0r0i1
                                                                                  [(make-recursion :nested-again [
                                                                                                                  (-->> "2" "A" "C") ;i0r0i1b00
                                                                                                                  (-->> "2" "C" "A") ;i0r0i1b01
                                                                                                                  (do-recur :nested-again)])] ;i0r0i1b02
                                                                                  [(-->> "4" "A" "B")
                                                                                   (do-recur :nested)]
                                                                                  [(-->> "3" "A" "D") ;i0r0i1b10
                                                                                   (end-recur :nested)] ;i0r0i1b11
                                                                                  ])
                                                                    (make-choice [;i0r0i2
                                                                                  [(-->> "2" "A" "C") ;i0r0i2b00
                                                                                   (-->> "2" "C" "E") ;i0r0i2b01
                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                  [(-->> "3" "A" "F") ; i0r0i2b10
                                                                                   (end-recur :test)] ;i0r0i2b11
                                                                                  ])
                                                                    ])]

                                    )                       ;i1
                    ]))

(defn two-buyer-protocol []
  (create-protocol [(make-recursion :order-book [ ;i0
                                           (-->> "title" "Buyer1" "Seller") ;i0r0i0
                                           (-->> "quote" "Seller" ["Buyer1" "Buyer2"]);i0r0i1
                                           (-->> "quoteDiv" "Buyer1" "Buyer2");i0r0i2
                                           (make-choice [;i0r0i3
                                                         [(-->> "ok" "Buyer2" "Seller");i0r0i3b00
                                                          (-->> "date" "Seller" "Buyer2");i0r0i3b01
                                                          (do-recur :order-book)];i0r0i3b02
                                                         [(-->> "quit" "Buyer2" "Seller");i0r0i3b10
                                                          (end-recur :order-book)]])]);i0r0i3b11
                    ]))