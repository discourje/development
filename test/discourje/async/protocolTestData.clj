(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [clj-uuid :as uuid]))

(deftest interactableTest
  (let [inter (make-interaction "1" "A" "B")]
    (is (= "1" (get-action inter)))
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleParallelProtocol []
  (create-protocol [(make-interaction "1" "A" ["B" "C"])]))

(def testSingleParallelProtocolControl
  [(->interaction (uuid/v1) "1" "A" ["B" "C"] nil)])

(defn testDualProtocol [include-ids]
  (if include-ids
    (create-protocol [(make-interaction "1" "A" "B")
                      (make-interaction "2" "B" "A")]))
  (create-protocol [(->interaction nil "1" "A" "B" nil)
                    (->interaction nil "2" "B" "A" nil)]))

(def testDualProtocolControl
  (->interaction nil "1" "A" "B"
                 (->interaction nil "2" "B" "A" nil)))

(defn test-typed-DualProtocol [include-ids]
  (when include-ids (create-protocol [(make-interaction java.lang.String "A" "B")
                                      (make-interaction java.lang.String "B" "A")])
                    (create-protocol [(->interaction nil java.lang.String "A" "B" nil)
                                      (->interaction nil java.lang.String "B" "A" nil)])))
(def test-typed-DualProtocolControl
  (->interaction nil java.lang.String "A" "B"
                 (->interaction nil java.lang.String "B" "A" nil)))

(defn testTripleProtocol [include-ids]
  (if include-ids
    (create-protocol [
                      (make-interaction "1" "A" "B")
                      (make-interaction "2" "B" "A")
                      (make-interaction "3" "A" "C")]))
  (create-protocol [
                    (->interaction nil "1" "A" "B" nil)
                    (->interaction nil "2" "B" "A" nil)
                    (->interaction nil "3" "A" "C" nil)]))

(def testTripleProtocolControl
  (->interaction nil "1" "A" "B"
                 (->interaction nil "2" "B" "A"
                                (->interaction nil "3" "A" "C" nil))))

(defn testParallelProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction "1" "A" "B")
                                    (make-interaction "2" "B" "A")
                                    (make-interaction "3" "A" "C")
                                    (make-interaction "4" "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil "1" "A" "B" nil)
                                    (->interaction nil "2" "B" "A" nil)
                                    (->interaction nil "3" "A" "C" nil)
                                    (->interaction nil "4" "C" ["A" "B"] nil)])))
(def testParallelProtocolControl
  (->interaction nil "1" "A" "B"
                 (->interaction nil "2" "B" "A"
                                (->interaction nil "3" "A" "C"
                                               (->interaction nil "4" "C" ["A" "B"] nil)))))

(defn testQuadProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction "start" "main" ["A" "B" "C"])
                                    (make-interaction "1" "A" "B")
                                    (make-interaction "2" "B" "A")
                                    (make-interaction "3" "A" "C")
                                    (make-interaction "4" "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil "start" "main" ["A" "B" "C"] nil)
                                    (->interaction nil "1" "A" "B" nil)
                                    (->interaction nil "2" "B" "A" nil)
                                    (->interaction nil "3" "A" "C" nil)
                                    (->interaction nil "4" "C" ["A" "B"] nil)])))
(def testQuadProtocolControl
  (->interaction nil "start" "main" ["A" "B" "C"]
                 (->interaction nil "1" "A" "B"
                                (->interaction nil "2" "B" "A"
                                               (->interaction nil "3" "A" "C"
                                                              (->interaction nil "4" "C" ["A" "B"] nil))))))

(defn tesParallelParticipantsProtocol []
  (mep (-->> "1" "A" ["B" "C"])
       (-->> "2" "B" "A")))

(defn tesParallelParticipantsWithChoiceProtocol []
  (mep (-->> "1" "A" "B")
       (-->> "2" "B" "A")
       (choice
         [(-->> "3" "A" ["B" "C"])]
         [(-->> "5" "A" ["B" "C"])])
       (-->> "4" "B" "A")))

(defn single-choice-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-interaction "hi" "A" "C")]]
                                 )]))

(defn single-choice-in-middle-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction "99" "Start" "Finish")
                                    (make-choice [
                                                  [(make-interaction "1" "A" "B")
                                                   (make-interaction "bla" "B" "A")]
                                                  [(make-interaction "2" "A" "C")
                                                   (make-interaction "hello" "C" "A")]]
                                                 )
                                    (make-interaction "88" "Finish" "Start")])
                  (create-protocol [(->interaction nil "99" "Start" "Finish" nil)
                                    (->branch nil [
                                                   [(->interaction nil "1" "A" "B" nil)
                                                    (->interaction nil "bla" "B" "A" nil)]
                                                   [(->interaction nil "2" "A" "C" nil)
                                                    (->interaction nil "hello" "C" "A" nil)]]
                                              nil)
                                    (->interaction nil "88" "Finish" "Start" nil)])))
(def single-choice-in-middle-protocolControl
  (->interaction nil "99" "Start" "Finish"
                 (->branch nil [
                                (->interaction nil "1" "A" "B"
                                               (->interaction nil "bla" "B" "A"
                                                              (->interaction nil "88" "Finish" "Start" nil)))
                                (->interaction nil "2" "A" "C"
                                               (->interaction nil "hello" "C" "A"
                                                              (->interaction nil "88" "Finish" "Start" nil)))]
                           nil)))

(defn single-choice-5branches-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction "1" "A" "B")]
                                                  [(make-interaction "1" "A" "C")]
                                                  [(make-interaction "1" "A" "D")]
                                                  [(make-interaction "1" "A" "E")]
                                                  [(make-interaction "1" "A" "F")]
                                                  ]
                                                 )
                                    (make-interaction "Done" "A" "End")])
                  (create-protocol [(->branch nil [
                                                   [(->interaction nil "1" "A" "B" nil)]
                                                   [(->interaction nil "1" "A" "C" nil)]
                                                   [(->interaction nil "1" "A" "D" nil)]
                                                   [(->interaction nil "1" "A" "E" nil)]
                                                   [(->interaction nil "1" "A" "F" nil)]
                                                   ]
                                              nil)
                                    (->interaction nil "Done" "A" "End" nil)])))
(def single-choice-5branches-protocolControl
  (->branch nil [
                 (->interaction nil "1" "A" "B" (->interaction nil "Done" "A" "End" nil))
                 (->interaction nil "1" "A" "C" (->interaction nil "Done" "A" "End" nil))
                 (->interaction nil "1" "A" "D" (->interaction nil "Done" "A" "End" nil))
                 (->interaction nil "1" "A" "E" (->interaction nil "Done" "A" "End" nil))
                 (->interaction nil "1" "A" "F" (->interaction nil "Done" "A" "End" nil))
                 ]
            nil))

(defn dual-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction "1" "A" "B")]
                                                  [(make-interaction "hi" "A" "C")
                                                   (make-choice [
                                                                 [(make-interaction "hiA" "C" "A")]
                                                                 [(make-interaction "hiD" "C" "D")]]
                                                                )]]
                                                 )
                                    (make-interaction "Done" "A" "End")]))
  (create-protocol [(->branch nil [
                                   [(->interaction nil "1" "A" "B" nil)]
                                   [(->interaction nil "hi" "A" "C" nil)
                                    (->branch nil [
                                                   [(->interaction nil "hiA" "C" "A" nil)]
                                                   [(->interaction nil "hiD" "C" "D" nil)]]
                                              nil)]]
                              nil)
                    (->interaction nil "Done" "A" "End" nil)]))

(def dual-choice-protocolControl
  (->branch nil [
                 (->interaction nil "1" "A" "B" (->interaction nil "Done" "A" "End" nil))
                 (->interaction nil "hi" "A" "C"
                                (->branch nil [
                                               (->interaction nil "hiA" "C" "A" (->interaction nil "Done" "A" "End" nil))
                                               (->interaction nil "hiD" "C" "D" (->interaction nil "Done" "A" "End" nil))]
                                          nil))]
            nil))

(defn single-choice-multiple-interactions-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction "1" "A" "B")
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
                                    ])
                  (create-protocol [(->interaction nil "1" "A" "B" nil)
                                    (->interaction nil "1" "B" "A" nil)
                                    (->branch nil [
                                                   [(->interaction nil "2" "A" "C" nil)
                                                    (->interaction nil "2" "C" "A" nil)
                                                    (->interaction nil "3" "A" "C" nil)
                                                    (->interaction nil "3" "C" "A" nil)]
                                                   [(->interaction nil "2" "A" "B" nil)
                                                    (->interaction nil "2" "B" "A" nil)
                                                    (->interaction nil "3" "A" "B" nil)
                                                    (->interaction nil "3" "B" "A" nil)]] nil)
                                    (->interaction nil "4" "A" "D" nil)
                                    (->interaction nil "4" "D" "A" nil)
                                    (->interaction nil "5" "A" ["B" "C" "D"] nil)
                                    ])))
(def single-choice-multiple-interactions-protocolControl
  (->interaction nil "1" "A" "B"
                 (->interaction nil "1" "B" "A"
                                (->branch nil [
                                               (->interaction nil "2" "A" "C"
                                                              (->interaction nil "2" "C" "A"
                                                                             (->interaction nil "3" "A" "C"
                                                                                            (->interaction nil "3" "C" "A" (->interaction nil "4" "A" "D"
                                                                                                                                          (->interaction nil "4" "D" "A"
                                                                                                                                                         (->interaction nil "5" "A" ["B" "C" "D"] nil)))))))
                                               (->interaction nil "2" "A" "B"
                                                              (->interaction nil "2" "B" "A"
                                                                             (->interaction nil "3" "A" "B"
                                                                                            (->interaction nil "3" "B" "A" (->interaction nil "4" "A" "D"
                                                                                                                                          (->interaction nil "4" "D" "A"
                                                                                                                                                         (->interaction nil "5" "A" ["B" "C" "D"] nil)))))))] nil)
                                )))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction "1" "A" "B")]
                                  [(make-choice [
                                                 [(make-interaction "1" "A" "C")]
                                                 [(make-interaction "1" "A" "D")]]
                                                )]]
                                 )
                    (make-interaction "Done" "A" "End")]))

(defn multiple-nested-choice-branch-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [;i0
                                                  [(make-choice [;i0b00
                                                                 [(make-interaction "1" "A" "B")] ;i0b00b00
                                                                 [(make-interaction "2" "A" "B")]] ;i0b00b10
                                                                )]
                                                  [(make-choice [;i0b10
                                                                 [(make-interaction "3" "A" "B")] ;i0b10b00
                                                                 [(make-interaction "4" "A" "B")]] ;i0b10b10
                                                                )]]
                                                 )])
                  (create-protocol [(->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil "1" "A" "B" nil)]
                                                                   [(->interaction nil "2" "A" "B" nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->interaction nil "3" "A" "B" nil)]
                                                                   [(->interaction nil "4" "A" "B" nil)]]
                                                              nil)]]
                                              nil)])))
(def multiple-nested-choice-branch-protocolControl
  (->branch nil [;i0
                 (->branch nil [;i0b00
                                (->interaction nil "1" "A" "B" nil)
                                (->interaction nil "2" "A" "B" nil)]
                           nil)
                 (->branch nil [;i0b10
                                (->interaction nil "3" "A" "B" nil)
                                (->interaction nil "4" "A" "B" nil)]
                           nil)]
            nil))

(defn multiple-nested-branches-protocol [include-ids]
  (if include-ids (create-protocol [
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
                                    (make-interaction "Done" "A" "End")] ;i1
                                   )
                  (create-protocol [
                                    (->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil "1" "A" "B" nil)
                                                                    (->interaction nil "2" "B" "A" nil)]
                                                                   [(->interaction nil "1" "A" "C" nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->branch nil [
                                                                                   [(->branch nil [
                                                                                                   [(->interaction nil "1" "A" "D" nil)]
                                                                                                   [(->interaction nil "1" "A" ["E" "F" "G"] nil)
                                                                                                    (->interaction nil "3" "F" "A" nil)
                                                                                                    (->interaction nil "4" "G" "A" nil)]]
                                                                                              nil)]
                                                                                   [(->interaction nil "1" "A" "H" nil)]]
                                                                              nil)]
                                                                   [(->interaction nil "1" "A" "I" nil)]]
                                                              nil)]]
                                              nil)
                                    (->interaction nil "Done" "A" "End" nil)]
                                   )))

(def multiple-nested-branches-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil "1" "A" "B"
                                               (->interaction nil "2" "B" "A" (->interaction nil "Done" "A" "End" nil)))
                                (->interaction nil "1" "A" "C" (->interaction nil "Done" "A" "End" nil))]
                           nil)
                 (->branch nil [
                                (->branch nil [
                                               (->branch nil [
                                                              (->interaction nil "1" "A" "D" (->interaction nil "Done" "A" "End" nil))
                                                              (->interaction nil "1" "A" ["E" "F" "G"]
                                                                             (->interaction nil "3" "F" "A"
                                                                                            (->interaction nil "4" "G" "A" (->interaction nil "Done" "A" "End" nil))))]
                                                         nil)
                                               (->interaction nil "1" "A" "H" (->interaction nil "Done" "A" "End" nil))]
                                          nil)
                                (->interaction nil "1" "A" "I" (->interaction nil "Done" "A" "End" nil))]
                           nil)]
            nil)
  )


(defn single-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction "1" "A" "B") ;i0
                                    (make-recursion :test [;i1
                                                           (make-interaction "1" "B" "A") ; i1r0
                                                           (make-choice [;i1r1
                                                                         [(make-interaction "2" "A" "C") ;i1r1b00
                                                                          (make-interaction "2" "C" "A") ;i1r1b01
                                                                          (do-recur :test)] ;i1r1b02
                                                                         [(make-interaction "3" "A" "B") ;i1r1b10
                                                                          ]
                                                                         ])
                                                           ])
                                    (make-interaction "end" "A" ["B" "C"]) ; i2
                                    ])
                  (create-protocol [(->interaction nil "1" "A" "B" nil)
                                    (->recursion nil :test [
                                                            (->interaction nil "1" "B" "A" nil)
                                                            (->branch nil [
                                                                           [(->interaction nil "2" "A" "C" nil)
                                                                            (->interaction nil "2" "C" "A" nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil "3" "A" "B" nil)]] nil)
                                                            ] nil)
                                    (->interaction nil "end" "A" ["B" "C"] nil)
                                    ])))
(def single-recur-protocolControl
  (->interaction nil "1" "A" "B"
                 (->recursion nil :test
                              (->interaction nil "1" "B" "A"
                                             (->branch nil [
                                                            (->interaction nil "2" "A" "C"
                                                                           (->interaction nil "2" "C" "A"
                                                                                          (->recur-identifier nil :test :recur nil)))
                                                            (->interaction nil "3" "A" "B" (->interaction nil "end" "A" ["B" "C"] nil))

                                                            ] nil))
                              nil))
  )

(defn single-recur-one-choice-protocol []
  (create-protocol [(make-recursion :generate [
                                               (make-interaction "1" "A" "B")
                                               (make-choice [
                                                             [(make-interaction "2" "B" "A")
                                                              (do-recur :generate)]
                                                             [(make-interaction "3" "B" "A")]
                                                             ])
                                               ])
                    ]))

(defn one-recur-with-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [
                                                                         [(make-interaction "2" "A" "C")
                                                                          (do-recur :test)]
                                                                         [(make-interaction "3" "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    ])
                  (create-protocol [(->recursion nil :test [
                                                            (->branch nil [
                                                                           [(->interaction nil "2" "A" "C" nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil "3" "A" "B" nil)
                                                                            ]
                                                                           ] nil)
                                                            ] nil)
                                    ])
                  ))
(def one-recur-with-choice-protocolControl
  (->recursion nil :test
               (->branch nil [
                              (->interaction nil "2" "A" "C"
                                             (->recur-identifier nil :test :recur nil))
                              (->interaction nil "3" "A" "B" nil)
                              ] nil)
               nil))

(defn one-recur-with-startchoice-and-endchoice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [;i0
                                                  [(make-recursion :test [;i0b0r0
                                                                          (make-choice [;i0b0r00
                                                                                        [(make-interaction "2" "A" "C") ;i0b0r00b00
                                                                                         (do-recur :test)] ;i0b0r00b01
                                                                                        [(make-interaction "3" "A" "B")
                                                                                         ]
                                                                                        ])
                                                                          ])
                                                   ]
                                                  [(make-interaction "2" "A" "C")] ;i0b10
                                                  ])
                                    ])
                  (create-protocol [(->branch nil [
                                                   [(->recursion nil :test [
                                                                            (->branch nil [
                                                                                           [(->interaction nil "2" "A" "C" nil)
                                                                                            (->recur-identifier nil :test :recur nil)]
                                                                                           [(->interaction nil "3" "A" "B" nil)
                                                                                            ]
                                                                                           ] nil)
                                                                            ] nil)
                                                    ]
                                                   [(->interaction nil "2" "A" "C" nil)]
                                                   ] nil)
                                    ])))

(def one-recur-with-startchoice-and-endchoice-protocolControl
  (->branch nil [
                 (->recursion nil :test
                              (->branch nil [
                                             (->interaction nil "2" "A" "C"
                                                            (->recur-identifier nil :test :recur nil))
                                             (->interaction nil "3" "A" "B" nil)
                                             ] nil)
                              nil)
                 (->interaction nil "2" "A" "C" nil)
                 ] nil))


(defn nested-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [;i0
                                                           (make-recursion :nested [; i0r0
                                                                                    (make-interaction "1" "B" "A") ;i0r0i0
                                                                                    (make-choice [;i0r0i1
                                                                                                  [(make-interaction "2" "A" "C") ;i0r0i1b00
                                                                                                   (make-interaction "2" "C" "A") ;i0r0i1b01
                                                                                                   (do-recur :nested)] ;i0r0i1b02
                                                                                                  [(make-interaction "3" "A" "B")] ;i0r0i1b11
                                                                                                  ])
                                                                                    (make-choice [;i0r0i2
                                                                                                  [(make-interaction "2" "A" "C") ;i0r0i2b00
                                                                                                   (make-interaction "2" "C" "D") ;i0r0i2b01
                                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                                  [(make-interaction "3" "A" "E")] ;i0r0i2b11
                                                                                                  ])
                                                                                    ])]

                                                    )
                                    (make-interaction "end" "A" ["B" "C"]) ;i1
                                    ])
                  (create-protocol [(->recursion nil :test [;i0
                                                            (->recursion nil :nested [; i0r0
                                                                                      (->interaction nil "1" "B" "A" nil) ;i0r0i0
                                                                                      (->branch nil [;i0r0i1
                                                                                                     [(->interaction nil "2" "A" "C" nil)
                                                                                                      (->interaction nil "2" "C" "A" nil)
                                                                                                      (->recur-identifier nil :nested :recur nil)]
                                                                                                     [(->interaction nil "3" "A" "B" nil)]
                                                                                                     ] nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil "2" "A" "C" nil)
                                                                                                      (->interaction nil "2" "C" "D" nil)
                                                                                                      (->recur-identifier nil :test :recur nil)]
                                                                                                     [(->interaction nil "3" "A" "E" nil)]
                                                                                                     ] nil)
                                                                                      ] nil)]

                                                 nil)
                                    (->interaction nil "end" "A" ["B" "C"] nil)])))
(def nested-recur-protocolControl
  (->recursion nil :test
               (->recursion nil :nested                     ; i0r0
                            (->interaction nil "1" "B" "A"
                                           (->branch nil [;i0r0i1
                                                          (->interaction nil "2" "A" "C"
                                                                         (->interaction nil "2" "C" "A"
                                                                                        (->recur-identifier nil :nested :recur nil)))
                                                          (->interaction nil "3" "A" "B" (->branch nil [
                                                                                                        (->interaction nil "2" "A" "C"
                                                                                                                       (->interaction nil "2" "C" "D"
                                                                                                                                      (->recur-identifier nil :test :recur nil)))
                                                                                                        (->interaction nil "3" "A" "E" (->interaction nil "end" "A" ["B" "C"] nil))
                                                                                                        ] nil))
                                                          ] nil))

                            nil)

               nil)
  )

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
                                                                                  [(make-interaction "3" "A" "D")] ;i0r0i1b11
                                                                                  ])
                                                                    (make-choice [;i0r0i2
                                                                                  [(make-interaction "2" "A" "C") ;i0r0i2b00
                                                                                   (make-interaction "2" "C" "E") ;i0r0i2b01
                                                                                   (do-recur :test)] ;i0r0i2b02
                                                                                  [(make-interaction "3" "A" "F")] ;i0r0i2b11
                                                                                  ])
                                                                    ])]

                                    )                       ;i1
                    ]))

(defn two-buyer-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :order-book [
                                                                 (make-interaction "title" "Buyer1" "Seller")
                                                                 (make-interaction "quote" "Seller" ["Buyer1" "Buyer2"])
                                                                 (make-interaction "quoteDiv" "Buyer1" "Buyer2")
                                                                 (make-choice [
                                                                               [(make-interaction "ok" "Buyer2" "Seller")
                                                                                (make-interaction "date" "Seller" "Buyer2")
                                                                                (do-recur :order-book)]
                                                                               [(make-interaction "quit" "Buyer2" "Seller")]])])
                                    ])
                  (create-protocol [(->recursion nil :order-book [
                                                                  (->interaction nil "title" "Buyer1" "Seller" nil)
                                                                  (->interaction nil "quote" "Seller" ["Buyer1" "Buyer2"] nil)
                                                                  (->interaction nil "quoteDiv" "Buyer1" "Buyer2" nil)
                                                                  (->branch nil [
                                                                                 [(->interaction nil "ok" "Buyer2" "Seller" nil)
                                                                                  (->interaction nil "date" "Seller" "Buyer2" nil)
                                                                                  (->recur-identifier nil :order-book :recur nil)]
                                                                                 [(->interaction nil "quit" "Buyer2" "Seller" nil)]] nil)] nil)
                                    ])))
(def two-buyer-protocolControl
  (->recursion nil :order-book
               (->interaction nil "title" "Buyer1" "Seller"
                              (->interaction nil "quote" "Seller" ["Buyer1" "Buyer2"]
                                             (->interaction nil "quoteDiv" "Buyer1" "Buyer2"
                                                            (->branch nil [
                                                                           (->interaction nil "ok" "Buyer2" "Seller"
                                                                                          (->interaction nil "date" "Seller" "Buyer2"
                                                                                                         (->recur-identifier nil :order-book :recur nil)))
                                                                           (->interaction nil "quit" "Buyer2" "Seller" nil)] nil))))
               nil))
(defn parallel-after-interaction [include-ids]
  (if include-ids (create-protocol [(-->> 1 "a" "b")
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]
                                                    ])])
                  (create-protocol [(->interaction nil 1 "a" "b" nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)])))

(def parallel-after-interactionControl
  (->interaction nil 1 "a" "b"
                 (->parallel nil [(->interaction nil 2 "b" "a"
                                                 (->interaction nil 3 "a" "b" nil))
                                  (->interaction nil 4 "b" "a"
                                                 (->interaction nil 5 "a" "b" nil))
                                  ] nil)))

(defn parallel-after-interaction-with-after [include-ids]
  (if include-ids (create-protocol [(make-interaction 1 "a" "b")
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]
                                                    ])
                                    (make-interaction 6 "b" "a")])
                  (create-protocol [(->interaction nil 1 "a" "b" nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)
                                    (->interaction nil 6 "b" "a" nil)])))

(def parallel-after-interaction-with-afterControl
  (->interaction nil 1 "a" "b"
                 (->parallel nil [(->interaction nil 2 "b" "a"
                                                 (->interaction nil 3 "a" "b" nil))
                                  (->interaction nil 4 "b" "a"
                                                 (->interaction nil 5 "a" "b" nil))
                                  ] (->interaction nil 6 "b" "a" nil))))

(defn parallel-after-choice-with-after [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction 1 "a" "b")]
                                                  [(make-interaction 0 "a" "b")]])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-interaction 6 "b" "a")])
                  (create-protocol [(->branch nil [[(->interaction nil 1 "a" "b" nil)]
                                                   [(->interaction nil 0 "a" "b" nil)]] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)
                                    (->interaction nil 6 "b" "a" nil)])))

(def parallel-after-choice-with-afterControl
  (->branch nil [(->interaction nil 1 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                              (->interaction nil 3 "a" "b" nil))
                                                               (->interaction nil 4 "b" "a"
                                                                              (->interaction nil 5 "a" "b" nil))
                                                               ] (->interaction nil 6 "b" "a" nil)))
                 (->interaction nil 0 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                              (->interaction nil 3 "a" "b" nil))
                                                               (->interaction nil 4 "b" "a"
                                                                              (->interaction nil 5 "a" "b" nil))
                                                               ] (->interaction nil 6 "b" "a" nil)))] nil))

(defn parallel-after-choice-with-after-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction 1 "a" "b")]
                                                  [(make-interaction 0 "a" "b")]])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-choice [[(make-interaction 6 "b" "a")]
                                                  [(make-interaction 7 "b" "a")]])])
                  (create-protocol [(->branch nil [[(->interaction nil 1 "a" "b" nil)]
                                                   [(->interaction nil 0 "a" "b" nil)]] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)
                                    (->branch nil [[(->interaction nil 6 "b" "a" nil)]
                                                   [(->interaction nil 7 "b" "a" nil)]] nil)])))
(def parallel-after-choice-with-after-choiceControl
  (->branch nil [(->interaction nil 1 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                              (->interaction nil 3 "a" "b" nil))
                                                               (->interaction nil 4 "b" "a"
                                                                              (->interaction nil 5 "a" "b" nil))
                                                               ] (->branch nil [(->interaction nil 6 "b" "a" nil)
                                                                                (->interaction nil 7 "b" "a" nil)] nil)))
                 (->interaction nil 0 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                              (->interaction nil 3 "a" "b" nil))
                                                               (->interaction nil 4 "b" "a"
                                                                              (->interaction nil 5 "a" "b" nil))
                                                               ] (->branch nil [(->interaction nil 6 "b" "a" nil)
                                                                                (->interaction nil 7 "b" "a" nil)] nil)))] nil))

(defn parallel-after-rec-with-after [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                    (make-choice [[(make-interaction 1 "a" "b")
                                                                   (do-recur :test)]
                                                                  [(make-interaction 0 "a" "b")]])])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-choice [[(make-interaction 6 "b" "a")]
                                                  [(make-interaction 7 "b" "a")]])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil 1 "a" "b" nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil 0 "a" "b" nil)]] nil)] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)
                                    (->branch nil [[(->interaction nil 6 "b" "a" nil)]
                                                   [(->interaction nil 7 "b" "a" nil)]] nil)])))
(def parallel-after-rec-with-afterControl
  (->recursion nil :test (->branch nil [(->interaction nil 1 "a" "b" (->recur-identifier nil :test :recur nil))
                                        (->interaction nil 0 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                                                     (->interaction nil 3 "a" "b" nil))
                                                                                      (->interaction nil 4 "b" "a"
                                                                                                     (->interaction nil 5 "a" "b" nil))
                                                                                      ] (->branch nil [(->interaction nil 6 "b" "a" nil)
                                                                                                       (->interaction nil 7 "b" "a" nil)] nil)))] nil) nil))

(defn parallel-after-rec-with-after-rec [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                    (choice [[(make-interaction 1 "a" "b")
                                                              (do-recur :test)]
                                                             [(make-interaction 0 "a" "b")]])])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-recursion :test2 [
                                                            (choice [[(make-interaction 6 "b" "a")
                                                                      (do-recur :test2)]
                                                                     [(make-interaction 7 "b" "a")]])])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil 1 "a" "b" nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil 0 "a" "b" nil)]] nil)] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                      (->interaction nil 3 "a" "b" nil)]
                                                     [(->interaction nil 4 "b" "a" nil)
                                                      (->interaction nil 5 "a" "b" nil)]
                                                     ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil 6 "b" "a" nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil 7 "b" "a" nil)]] nil)] nil)])))

(def parallel-after-rec-with-after-recControl
  (->recursion nil :test (->branch nil [(->interaction nil 1 "a" "b" (->recur-identifier nil :test :recur nil))
                                        (->interaction nil 0 "a" "b" (->parallel nil [(->interaction nil 2 "b" "a"
                                                                                                     (->interaction nil 3 "a" "b" nil))
                                                                                      (->interaction nil 4 "b" "a"
                                                                                                     (->interaction nil 5 "a" "b" nil))
                                                                                      ] (->recursion nil :test2
                                                                                                     (->branch nil [(->interaction nil 6 "b" "a"
                                                                                                                                   (->recur-identifier nil :test2 :recur nil))
                                                                                                                    (->interaction nil 7 "b" "a" nil)] nil) nil)))] nil) nil))
(defn nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction 1 "a" "b")
                                    (make-parallel [[(make-parallel [[(make-interaction "a" "b" "a")
                                                                      (make-interaction "b" "a" "b")]
                                                                     [(make-interaction "b" "b" "a")
                                                                      (make-interaction "a" "a" "b")]])]
                                                    [(make-parallel [[(make-interaction 2 "b" "a")
                                                                      (make-interaction 3 "a" "b")]
                                                                     [(make-interaction 4 "b" "a")
                                                                      (make-interaction 5 "a" "b")]])]])])
                  (create-protocol [(->interaction nil 1 "a" "b" nil)
                                    (->parallel nil [[(->parallel nil [[(->interaction nil "a" "b" "a" nil)
                                                                        (->interaction nil "b" "a" "b" nil)]
                                                                       [(->interaction nil "b" "b" "a" nil)
                                                                        (->interaction nil "a" "a" "b" nil)]] nil)]
                                                     [(->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                                        (->interaction nil 3 "a" "b" nil)]
                                                                       [(->interaction nil 4 "b" "a" nil)
                                                                        (->interaction nil 5 "a" "b" nil)]] nil)]] nil)])))

(def nested-parallelControl (->interaction nil 1 "a" "b"
                                           (->parallel nil [(->parallel nil [(->interaction nil "a" "b" "a"
                                                                                            (->interaction nil "b" "a" "b" nil))
                                                                             (->interaction nil "b" "b" "a"
                                                                                            (->interaction nil "a" "a" "b" nil))] nil)
                                                            (->parallel nil [(->interaction nil 2 "b" "a"
                                                                                            (->interaction nil 3 "a" "b" nil))
                                                                             (->interaction nil 4 "b" "a"
                                                                                            (->interaction nil 5 "a" "b" nil))] nil)] nil)))
(defn after-parallel-nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-interaction 0 "b" "a")
                                                     (make-interaction 1 "a" "b")]
                                                    [(make-interaction "hi" "b" "a")
                                                     (make-interaction "hi" "a" "b")]])
                                    (make-parallel [[(make-parallel [[(make-interaction "a" "b" "a")
                                                                      (make-interaction "b" "a" "b")]
                                                                     [(make-interaction "b" "b" "a")
                                                                      (make-interaction "a" "a" "b")]])]
                                                    [(make-parallel [[(make-interaction 2 "b" "a")
                                                                      (make-interaction 3 "a" "b")]
                                                                     [(make-interaction 4 "b" "a")
                                                                      (make-interaction 5 "a" "b")]])]])])
                  (create-protocol [(->parallel nil [[(->interaction nil 0 "b" "a" nil)
                                                      (->interaction nil 1 "a" "b" nil)]
                                                     [(->interaction nil "hi" "b" "a" nil)
                                                      (->interaction nil "hi" "a" "b" nil)]] nil)
                                    (->parallel nil [[(->parallel nil [[(->interaction nil "a" "b" "a" nil)
                                                                        (->interaction nil "b" "a" "b" nil)]
                                                                       [(->interaction nil "b" "b" "a" nil)
                                                                        (->interaction nil "a" "a" "b" nil)]] nil)]
                                                     [(->parallel nil [[(->interaction nil 2 "b" "a" nil)
                                                                        (->interaction nil 3 "a" "b" nil)]
                                                                       [(->interaction nil 4 "b" "a" nil)
                                                                        (->interaction nil 5 "a" "b" nil)]] nil)]] nil)])))
(def after-parallel-nested-parallelControl (->parallel nil [(->interaction nil 0 "b" "a"
                                                                           (->interaction nil 1 "a" "b" nil))
                                                            (->interaction nil "hi" "b" "a"
                                                                           (->interaction nil "hi" "a" "b" nil))]
                                                       (->parallel nil [(->parallel nil [(->interaction nil "a" "b" "a"
                                                                                                        (->interaction nil "b" "a" "b" nil))
                                                                                         (->interaction nil "b" "b" "a"
                                                                                                        (->interaction nil "a" "a" "b" nil))] nil)
                                                                        (->parallel nil [(->interaction nil 2 "b" "a"
                                                                                                        (->interaction nil 3 "a" "b" nil))
                                                                                         (->interaction nil 4 "b" "a"
                                                                                                        (->interaction nil 5 "a" "b" nil))] nil)] nil)))