(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async.async :refer :all]))

(deftest interactableTest
  (let [inter (make-interaction "1" "A" "B")]
    (is (= "1" (get-action inter)))
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleParallelProtocol []
  (create-protocol [(make-interaction "1" "A" ["B" "C"])]))

(defn testDualProtocol []
  (create-protocol [(make-interaction "1" "A" "B")
                    (make-interaction "2" "B" "A")]))

(defn test-typed-DualProtocol []
  (create-protocol [(make-interaction java.lang.String "A" "B")
                    (make-interaction java.lang.String "B" "A")]))

(defn testTripleProtocol []
  (create-protocol [
                    (make-interaction "1" "A" "B")
                    (make-interaction "2" "B" "A")
                    (make-interaction "3" "A" "C")]))

(defn testParallelProtocol []
  (create-protocol [
                    (make-interaction "1" "A" "B")
                    (make-interaction "2" "B" "A")
                    (make-interaction "3" "A" "C")
                    (make-interaction "4" "C" ["A" "B"])]))

(defn testQuadProtocol []
  (create-protocol [
                    (make-interaction "start" "main" ["A" "B" "C"])
                    (make-interaction "1" "A" "B")
                    (make-interaction "2" "B" "A")
                    (make-interaction "3" "A" "C")
                    (make-interaction "4" "C" ["A" "B"])]))

(defn single-choice-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-interaction "hi" "A" "C")]]
                                 )]))

(defn single-choice-in-middle-protocol []
  (create-protocol [(make-interaction "99" "Start" "Finish")
                    (make-choice [
                                  [(make-interaction "1" "A" "B")
                                   (make-interaction "bla" "B" "A")]
                                  [(make-interaction "2" "A" "C")
                                   (make-interaction "hello" "C" "A")]]
                                 )
                    (make-interaction "88" "Finish" "Start")]))

(defn single-choice-5branches-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-interaction "1" "A" "C")]
                                  [(make-interaction "1" "A" "D")]
                                  [(make-interaction "1" "A" "E")]
                                  [(make-interaction "1" "A" "F")]
                                  ]
                                 )
                    (make-interaction "Done" "A" "End")]))

(defn dual-choice-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-interaction "hi" "A" "C")
                                   (make-choice [
                                                 [(make-interaction "hiA" "C" "A")]
                                                 [(make-interaction "hiD" "C" "D")]]
                                                )]]
                                 )
                    (make-interaction "Done" "A" "End")]))

(defn single-choice-multiple-interactions-protocol []
  (create-protocol [(make-interaction "1" "A" "B")
                    (make-interaction "1" "B" "A")
                    (make-choice [
                                  [(make-interaction "2" "A" "C")
                                   (make-interaction "2" "C" "A")
                                   (make-interaction "3" "A" "C")
                                   (make-interaction "3" "C" "A")]
                                  [(make-interaction "2" "A" "B")
                                   (make-interaction "2" "B" "A")
                                   (make-interaction "3" "A" "B")
                                   (make-interaction "3" "B" "A")]])
                    (make-interaction "4" "A" "D")
                    (make-interaction "4" "D" "A")
                    (make-interaction "5" "A" ["B" "C" "D"])
                    ]))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-choice [
                                                 [(make-interaction "1" "A" "C")]
                                                 [(make-interaction "1" "A" "D")]]
                                                )]]
                                 )
                    (make-interaction "Done" "A" "End")]))

(defn multiple-nested-choice-branch-protocol []
  (create-protocol [(make-choice [;i0
                                  [(make-choice [;i0b00
                                                 [(make-interaction "1" "A" "B")] ;i0b00b00
                                                 [(make-interaction "2" "A" "B")]] ;i0b00b10
                                                )]
                                  [(make-choice [;i0b10
                                                 [(make-interaction "3" "A" "B")] ;i0b10b00
                                                 [(make-interaction "4" "A" "B")]] ;i0b10b10
                                                )]]
                                 )]))

(defn multiple-nested-branches-protocol []
  (create-protocol [
                    (make-choice [;i0
                                  [(make-choice [;i0b0
                                                 [(make-interaction "1" "A" "B") ;i0b0b00
                                                  (make-interaction "2" "B" "A")] ;i0b0b01
                                                 [(make-interaction "1" "A" "C")]] ;i0b0b10
                                                )]
                                  [(make-choice [;i0b1
                                                 [(make-choice [;i0b1b0
                                                                [(make-choice [;i0b1b0b0
                                                                               [(make-interaction "1" "A" "D")] ;i0b1b0b0b00
                                                                               [(make-interaction "1" "A" ["E" "F" "G"]) ;i0b1b0b0b10
                                                                                (make-interaction "3" "F" "A") ;i0b1b0b0b11
                                                                                (make-interaction "4" "G" "A")]] ;i0b1b0b0b12
                                                                              )]
                                                                [(make-interaction "1" "A" "H")]] ;i0b1b0b10
                                                               )]
                                                 [(make-interaction "1" "A" "I")]] ;i0b1b11
                                                )]]
                                 )
                    (make-interaction "Done" "A" "End")]                ;i1
                   ))

(defn single-recur-protocol []
  (create-protocol [(make-interaction "1" "A" "B")                      ;i0
                    (make-recursion :test [;i1
                                           (make-interaction "1" "B" "A") ; i1r0
                                           (make-choice [;i1r1
                                                         [(make-interaction "2" "A" "C") ;i1r1b00
                                                          (make-interaction "2" "C" "A") ;i1r1b01
                                                          (do-recur :test)] ;i1r1b02
                                                         [(make-interaction "3" "A" "B") ;i1r1b10
                                                          (end-recur :test) ;i1r1b11
                                                          ]
                                                         ])
                                           ])
                    (make-interaction "end" "A" ["B" "C"])              ; i2
                    ]))

(defn single-recur-one-choice-protocol []
  (create-protocol [(make-recursion :generate [
                                               (make-interaction "1" "A" "B")
                                               (make-choice [
                                                             [(make-interaction "2" "B" "A")
                                                              (do-recur :generate)]
                                                             [(make-interaction "3" "B" "A")
                                                              (end-recur :generate)]
                                                             ])
                                               ])
                    ]))

(defn one-recur-with-choice-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-choice [;i0r0
                                                         [(make-interaction "2" "A" "C") ;i0r0b00
                                                          (do-recur :test)] ;i0r0b01
                                                         [(make-interaction "3" "A" "B") ;i0r0b10
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
                                                                        [(make-interaction "2" "A" "C") ;i0b0r00b00
                                                                         (do-recur :test)] ;i0b0r00b01
                                                                        [(make-interaction "3" "A" "B") ;i0b0r00b10
                                                                         (end-recur :test) ;i0b0r00b11
                                                                         ]
                                                                        ])
                                                          ])
                                   ]
                                  [(make-interaction "2" "A" "C")]      ;i0b10
                                  ])
                    ]))


(defn nested-recur-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-recursion :nested [; i0r0
                                                                    (make-interaction "1" "B" "A") ;i0r0i0
                                                                    (make-choice [;i0r0i1
                                                                                  [(make-interaction "2" "A" "C") ;i0r0i1b00
                                                                                   (make-interaction "2" "C" "A") ;i0r0i1b01
                                                                                   (do-recur :nested)] ;i0r0i1b02
                                                                                  [(make-interaction "3" "A" "B") ;i0r0i1b10
                                                                                   (end-recur :nested)] ;i0r0i1b11
                                                                                  ])
                                                                    (make-choice [;i0r0i2
                                                                                  [(make-interaction "2" "A" "C") ;i0r0i2b00
                                                                                   (make-interaction "2" "C" "D") ;i0r0i2b01
                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                  [(make-interaction "3" "A" "E") ; i0r0i2b10
                                                                                   (end-recur :test)] ;i0r0i2b11
                                                                                  ])
                                                                    ])]

                                    )
                    (make-interaction "end" "A" ["B" "C"])              ;i1
                    ]))

(defn multiple-nested-recur-protocol []
  (create-protocol [(make-recursion :test [;i0
                                           (make-recursion :nested [; i0r0
                                                                    (make-choice [;i0r0i1
                                                                                  [(make-recursion :nested-again [
                                                                                                                  (make-interaction "2" "A" "C") ;i0r0i1b00
                                                                                                                  (make-interaction "2" "C" "A") ;i0r0i1b01
                                                                                                                  (do-recur :nested-again)])] ;i0r0i1b02
                                                                                  [(make-interaction "4" "A" "B")
                                                                                   (do-recur :nested)]
                                                                                  [(make-interaction "3" "A" "D") ;i0r0i1b10
                                                                                   (end-recur :nested)] ;i0r0i1b11
                                                                                  ])
                                                                    (make-choice [;i0r0i2
                                                                                  [(make-interaction "2" "A" "C") ;i0r0i2b00
                                                                                   (make-interaction "2" "C" "E") ;i0r0i2b01
                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                  [(make-interaction "3" "A" "F") ; i0r0i2b10
                                                                                   (end-recur :test)] ;i0r0i2b11
                                                                                  ])
                                                                    ])]

                                    )                       ;i1
                    ]))

(defn two-buyer-protocol []
  (create-protocol [(make-recursion :order-book [;i0
                                                 (make-interaction "title" "Buyer1" "Seller") ;i0r0i0
                                                 (make-interaction "quote" "Seller" ["Buyer1" "Buyer2"]) ;i0r0i1
                                                 (make-interaction "quoteDiv" "Buyer1" "Buyer2") ;i0r0i2
                                                 (make-choice [;i0r0i3
                                                               [(make-interaction "ok" "Buyer2" "Seller") ;i0r0i3b00
                                                                (make-interaction "date" "Seller" "Buyer2") ;i0r0i3b01
                                                                (do-recur :order-book)] ;i0r0i3b02
                                                               [(make-interaction "quit" "Buyer2" "Seller") ;i0r0i3b10
                                                                (end-recur :order-book)]])]) ;i0r0i3b11
                    ]))
