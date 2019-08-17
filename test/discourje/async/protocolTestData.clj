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
  [(->interaction (uuid/v1) "1" "A" ["B" "C"] false nil)])

(defn testDualProtocol [include-ids]
  (if include-ids
    (create-protocol [(make-interaction "1" "A" "B")
                      (make-interaction "2" "B" "A")]))
  (create-protocol [(->interaction nil "1" "A" "B" false nil)
                    (->interaction nil "2" "B" "A" false nil)]))

(def testDualProtocolControl
  (->interaction nil "1" "A" "B" false
                 (->interaction nil "2" "B" "A" false nil)))

(defn test-typed-DualProtocol [include-ids]
  (when include-ids (create-protocol [(make-interaction java.lang.String "A" "B")
                                      (make-interaction java.lang.String "B" "A")])
                    (create-protocol [(->interaction nil java.lang.String "A" "B" false nil)
                                      (->interaction nil java.lang.String "B" "A" false nil)])))
(def test-typed-DualProtocolControl
  (->interaction nil java.lang.String "A" "B" false
                 (->interaction nil java.lang.String "B" "A" false nil)))

(defn testTripleProtocol [include-ids]
  (if include-ids
    (create-protocol [
                      (make-interaction "1" "A" "B")
                      (make-interaction "2" "B" "A")
                      (make-interaction "3" "A" "C")]))
  (create-protocol [
                    (->interaction nil "1" "A" "B" false nil)
                    (->interaction nil "2" "B" "A" false nil)
                    (->interaction nil "3" "A" "C" false nil)]))

(def testTripleProtocolControl
  (->interaction nil "1" "A" "B" false
                 (->interaction nil "2" "B" "A" false
                                (->interaction nil "3" "A" "C" false nil))))

(defn testParallelProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction "1" "A" "B")
                                    (make-interaction "2" "B" "A")
                                    (make-interaction "3" "A" "C")
                                    (make-interaction "4" "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil "1" "A" "B" false nil)
                                    (->interaction nil "2" "B" "A" false nil)
                                    (->interaction nil "3" "A" "C" false nil)
                                    (->interaction nil "4" "C" ["A" "B"] false nil)])))
(def testParallelProtocolControl
  (->interaction nil "1" "A" "B" false
                 (->interaction nil "2" "B" "A" false
                                (->interaction nil "3" "A" "C" false
                                               (->interaction nil "4" "C" ["A" "B"] false nil)))))

(defn testQuadProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction "start" "main" ["A" "B" "C"])
                                    (make-interaction "1" "A" "B")
                                    (make-interaction "2" "B" "A")
                                    (make-interaction "3" "A" "C")
                                    (make-interaction "4" "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil "start" "main" ["A" "B" "C"] false nil)
                                    (->interaction nil "1" "A" "B" false nil)
                                    (->interaction nil "2" "B" "A" false nil)
                                    (->interaction nil "3" "A" "C" false nil)
                                    (->interaction nil "4" "C" ["A" "B"] false nil)])))
(def testQuadProtocolControl
  (->interaction nil "start" "main" ["A" "B" "C"] false
                 (->interaction nil "1" "A" "B" false
                                (->interaction nil "2" "B" "A" false
                                               (->interaction nil "3" "A" "C" false
                                                              (->interaction nil "4" "C" ["A" "B"] false nil))))))

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
                  (create-protocol [(->interaction nil "99" "Start" "Finish" false nil)
                                    (->branch nil [
                                                   [(->interaction nil "1" "A" "B" false nil)
                                                    (->interaction nil "bla" "B" "A" false nil)]
                                                   [(->interaction nil "2" "A" "C" false nil)
                                                    (->interaction nil "hello" "C" "A" false nil)]]
                                              nil)
                                    (->interaction nil "88" "Finish" "Start" false nil)])))
(def single-choice-in-middle-protocolControl
  (->interaction nil "99" "Start" "Finish" false
                 (->branch nil [
                                (->interaction nil "1" "A" "B" false
                                               (->interaction nil "bla" "B" "A" false
                                                              (->interaction nil "88" "Finish" "Start" false nil)))
                                (->interaction nil "2" "A" "C" false
                                               (->interaction nil "hello" "C" "A" false
                                                              (->interaction nil "88" "Finish" "Start" false nil)))]
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
                                                   [(->interaction nil "1" "A" "B" false nil)]
                                                   [(->interaction nil "1" "A" "C" false nil)]
                                                   [(->interaction nil "1" "A" "D" false nil)]
                                                   [(->interaction nil "1" "A" "E" false nil)]
                                                   [(->interaction nil "1" "A" "F" false nil)]
                                                   ]
                                              nil)
                                    (->interaction nil "Done" "A" "End" false nil)])))
(def single-choice-5branches-protocolControl
  (->branch nil [
                 (->interaction nil "1" "A" "B" false (->interaction nil "Done" "A" "End" false nil))
                 (->interaction nil "1" "A" "C" false (->interaction nil "Done" "A" "End" false nil))
                 (->interaction nil "1" "A" "D" false (->interaction nil "Done" "A" "End" false nil))
                 (->interaction nil "1" "A" "E" false (->interaction nil "Done" "A" "End" false nil))
                 (->interaction nil "1" "A" "F" false (->interaction nil "Done" "A" "End" false nil))
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
                                   [(->interaction nil "1" "A" "B" false nil)]
                                   [(->interaction nil "hi" "A" "C" false nil)
                                    (->branch nil [
                                                   [(->interaction nil "hiA" "C" "A" false nil)]
                                                   [(->interaction nil "hiD" "C" "D" false nil)]]
                                              nil)]]
                              nil)
                    (->interaction nil "Done" "A" "End" false nil)]))

(def dual-choice-protocolControl
  (->branch nil [
                 (->interaction nil "1" "A" "B" false (->interaction nil "Done" "A" "End" false nil))
                 (->interaction nil "hi" "A" "C" false
                                (->branch nil [
                                               (->interaction nil "hiA" "C" "A" false (->interaction nil "Done" "A" "End" false nil))
                                               (->interaction nil "hiD" "C" "D" false (->interaction nil "Done" "A" "End" false nil))]
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
                  (create-protocol [(->interaction nil "1" "A" "B" false nil)
                                    (->interaction nil "1" "B" "A" false nil)
                                    (->branch nil [
                                                   [(->interaction nil "2" "A" "C" false nil)
                                                    (->interaction nil "2" "C" "A" false nil)
                                                    (->interaction nil "3" "A" "C" false nil)
                                                    (->interaction nil "3" "C" "A" false nil)]
                                                   [(->interaction nil "2" "A" "B" false nil)
                                                    (->interaction nil "2" "B" "A" false nil)
                                                    (->interaction nil "3" "A" "B" false nil)
                                                    (->interaction nil "3" "B" "A" false nil)]] nil)
                                    (->interaction nil "4" "A" "D" false nil)
                                    (->interaction nil "4" "D" "A" false nil)
                                    (->interaction nil "5" "A" ["B" "C" "D"] false nil)
                                    ])))
(def single-choice-multiple-interactions-protocolControl
  (->interaction nil "1" "A" "B" false
                 (->interaction nil "1" "B" "A" false
                                (->branch nil [
                                               (->interaction nil "2" "A" "C" false
                                                              (->interaction nil "2" "C" "A" false
                                                                             (->interaction nil "3" "A" "C" false
                                                                                            (->interaction nil "3" "C" "A" false (->interaction nil "4" "A" "D" false
                                                                                                                                                (->interaction nil "4" "D" "A" false
                                                                                                                                                               (->interaction nil "5" "A" ["B" "C" "D"] false nil)))))))
                                               (->interaction nil "2" "A" "B" false
                                                              (->interaction nil "2" "B" "A" false
                                                                             (->interaction nil "3" "A" "B" false
                                                                                            (->interaction nil "3" "B" "A" false (->interaction nil "4" "A" "D" false
                                                                                                                                                (->interaction nil "4" "D" "A" false
                                                                                                                                                               (->interaction nil "5" "A" ["B" "C" "D"] false nil)))))))] nil)
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
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-choice [
                                                                 [(make-interaction "1" "A" "B")]
                                                                 [(make-interaction "2" "A" "B")]]
                                                                )]
                                                  [(make-choice [
                                                                 [(make-interaction "3" "A" "B")]
                                                                 [(make-interaction "4" "A" "B")]]
                                                                )]]
                                                 )])
                  (create-protocol [(->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil "1" "A" "B" false nil)]
                                                                   [(->interaction nil "2" "A" "B" false nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->interaction nil "3" "A" "B" false nil)]
                                                                   [(->interaction nil "4" "A" "B" false nil)]]
                                                              nil)]]
                                              nil)])))
(def multiple-nested-choice-branch-protocolControl
  (->branch nil [;i0
                 (->branch nil [;i0b00
                                (->interaction nil "1" "A" "B" false nil)
                                (->interaction nil "2" "A" "B" false nil)]
                           nil)
                 (->branch nil [;i0b10
                                (->interaction nil "3" "A" "B" false nil)
                                (->interaction nil "4" "A" "B" false nil)]
                           nil)]
            nil))

(defn multiple-nested-branches-protocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-choice [;i0
                                                  [(make-choice [
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
                                                                   [(->interaction nil "1" "A" "B" false nil)
                                                                    (->interaction nil "2" "B" "A" false nil)]
                                                                   [(->interaction nil "1" "A" "C" false nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->branch nil [
                                                                                   [(->branch nil [
                                                                                                   [(->interaction nil "1" "A" "D" false nil)]
                                                                                                   [(->interaction nil "1" "A" ["E" "F" "G"] false nil)
                                                                                                    (->interaction nil "3" "F" "A" false nil)
                                                                                                    (->interaction nil "4" "G" "A" false nil)]]
                                                                                              nil)]
                                                                                   [(->interaction nil "1" "A" "H" false nil)]]
                                                                              nil)]
                                                                   [(->interaction nil "1" "A" "I" false nil)]]
                                                              nil)]]
                                              nil)
                                    (->interaction nil "Done" "A" "End" false nil)]
                                   )))

(def multiple-nested-branches-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil "1" "A" "B" false
                                               (->interaction nil "2" "B" "A" false (->interaction nil "Done" "A" "End" false nil)))
                                (->interaction nil "1" "A" "C" false (->interaction nil "Done" "A" "End" false nil))]
                           nil)
                 (->branch nil [
                                (->branch nil [
                                               (->branch nil [
                                                              (->interaction nil "1" "A" "D" false (->interaction nil "Done" "A" "End" false nil))
                                                              (->interaction nil "1" "A" ["E" "F" "G"] false
                                                                             (->interaction nil "3" "F" "A" false
                                                                                            (->interaction nil "4" "G" "A" false (->interaction nil "Done" "A" "End" false nil))))]
                                                         nil)
                                               (->interaction nil "1" "A" "H" false (->interaction nil "Done" "A" "End" false nil))]
                                          nil)
                                (->interaction nil "1" "A" "I" false (->interaction nil "Done" "A" "End" false nil))]
                           nil)]
            nil)
  )


(defn single-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction "1" "A" "B")
                                    (make-recursion :test [
                                                           (make-interaction "1" "B" "A")
                                                           (make-choice [
                                                                         [(make-interaction "2" "A" "C")
                                                                          (make-interaction "2" "C" "A")
                                                                          (do-recur :test)]
                                                                         [(make-interaction "3" "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    (make-interaction "end" "A" ["B" "C"])
                                    ])
                  (create-protocol [(->interaction nil "1" "A" "B" false nil)
                                    (->recursion nil :test [
                                                            (->interaction nil "1" "B" "A" false nil)
                                                            (->branch nil [
                                                                           [(->interaction nil "2" "A" "C" false nil)
                                                                            (->interaction nil "2" "C" "A" false nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil "3" "A" "B" false nil)]] nil)
                                                            ] nil)
                                    (->interaction nil "end" "A" ["B" "C"] false nil)
                                    ])))
(def single-recur-protocolControl
  (->interaction nil "1" "A" "B" false
                 (->recursion nil :test
                              (->interaction nil "1" "B" "A" false
                                             (->branch nil [
                                                            (->interaction nil "2" "A" "C" false
                                                                           (->interaction nil "2" "C" "A" false
                                                                                          (->recur-identifier nil :test :recur nil)))
                                                            (->interaction nil "3" "A" "B" false (->interaction nil "end" "A" ["B" "C"] false nil))

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
                                                                           [(->interaction nil "2" "A" "C" false nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil "3" "A" "B" false nil)
                                                                            ]
                                                                           ] nil)
                                                            ] nil)
                                    ])
                  ))
(def one-recur-with-choice-protocolControl
  (->recursion nil :test
               (->branch nil [
                              (->interaction nil "2" "A" "C" false
                                             (->recur-identifier nil :test :recur nil))
                              (->interaction nil "3" "A" "B" false nil)
                              ] nil)
               nil))

(defn one-recur-with-startchoice-and-endchoice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-recursion :test [
                                                                          (make-choice [
                                                                                        [(make-interaction "2" "A" "C")
                                                                                         (do-recur :test)]
                                                                                        [(make-interaction "3" "A" "B")
                                                                                         ]
                                                                                        ])
                                                                          ])
                                                   ]
                                                  [(make-interaction "2" "A" "C")]
                                                  ])
                                    ])
                  (create-protocol [(->branch nil [
                                                   [(->recursion nil :test [
                                                                            (->branch nil [
                                                                                           [(->interaction nil "2" "A" "C" false nil)
                                                                                            (->recur-identifier nil :test :recur nil)]
                                                                                           [(->interaction nil "3" "A" "B" false nil)
                                                                                            ]
                                                                                           ] nil)
                                                                            ] nil)
                                                    ]
                                                   [(->interaction nil "2" "A" "C" false nil)]
                                                   ] nil)
                                    ])))

(def one-recur-with-startchoice-and-endchoice-protocolControl
  (->branch nil [
                 (->recursion nil :test
                              (->branch nil [
                                             (->interaction nil "2" "A" "C" false
                                                            (->recur-identifier nil :test :recur nil))
                                             (->interaction nil "3" "A" "B" false nil)
                                             ] nil)
                              nil)
                 (->interaction nil "2" "A" "C" false nil)
                 ] nil))


(defn nested-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-recursion :nested [
                                                                                    (make-interaction "1" "B" "A")
                                                                                    (make-choice [
                                                                                                  [(make-interaction "2" "A" "C")
                                                                                                   (make-interaction "2" "C" "A")
                                                                                                   (do-recur :nested)]
                                                                                                  [(make-interaction "3" "A" "B")]
                                                                                                  ])
                                                                                    (make-choice [
                                                                                                  [(make-interaction "2" "A" "C")
                                                                                                   (make-interaction "2" "C" "D")
                                                                                                   (do-recur :test)]
                                                                                                  [(make-interaction "3" "A" "E")]
                                                                                                  ])
                                                                                    ])]

                                                    )
                                    (make-interaction "end" "A" ["B" "C"])
                                    ])
                  (create-protocol [(->recursion nil :test [
                                                            (->recursion nil :nested [
                                                                                      (->interaction nil "1" "B" "A" false nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil "2" "A" "C" false nil)
                                                                                                      (->interaction nil "2" "C" "A" false nil)
                                                                                                      (->recur-identifier nil :nested :recur nil)]
                                                                                                     [(->interaction nil "3" "A" "B" false nil)]
                                                                                                     ] nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil "2" "A" "C" false nil)
                                                                                                      (->interaction nil "2" "C" "D" false nil)
                                                                                                      (->recur-identifier nil :test :recur nil)]
                                                                                                     [(->interaction nil "3" "A" "E" false nil)]
                                                                                                     ] nil)
                                                                                      ] nil)]

                                                 nil)
                                    (->interaction nil "end" "A" ["B" "C"] false nil)])))
(def nested-recur-protocolControl
  (->recursion nil :test
               (->recursion nil :nested
                            (->interaction nil "1" "B" "A" false
                                           (->branch nil [
                                                          (->interaction nil "2" "A" "C" false
                                                                         (->interaction nil "2" "C" "A" false
                                                                                        (->recur-identifier nil :nested :recur nil)))
                                                          (->interaction nil "3" "A" "B" false (->branch nil [
                                                                                                        (->interaction nil "2" "A" "C" false
                                                                                                                       (->interaction nil "2" "C" "D" false
                                                                                                                                      (->recur-identifier nil :test :recur nil)))
                                                                                                        (->interaction nil "3" "A" "E" false(->interaction nil "end" "A" ["B" "C"] false nil))
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
                                                                  (->interaction nil "title" "Buyer1" "Seller" false nil)
                                                                  (->interaction nil "quote" "Seller" ["Buyer1" "Buyer2"] false nil)
                                                                  (->interaction nil "quoteDiv" "Buyer1" "Buyer2" false nil)
                                                                  (->branch nil [
                                                                                 [(->interaction nil "ok" "Buyer2" "Seller" false nil)
                                                                                  (->interaction nil "date" "Seller" "Buyer2" false nil)
                                                                                  (->recur-identifier nil :order-book :recur nil)]
                                                                                 [(->interaction nil "quit" "Buyer2" "Seller" false nil)]] nil)] nil)
                                    ])))
(def two-buyer-protocolControl
  (->recursion nil :order-book
               (->interaction nil "title" "Buyer1" "Seller" false
                              (->interaction nil "quote" "Seller" ["Buyer1" "Buyer2"] false
                                             (->interaction nil "quoteDiv" "Buyer1" "Buyer2" false
                                                            (->branch nil [
                                                                           (->interaction nil "ok" "Buyer2" "Seller" false
                                                                                          (->interaction nil "date" "Seller" "Buyer2" false
                                                                                                         (->recur-identifier nil :order-book :recur nil)))
                                                                           (->interaction nil "quit" "Buyer2" "Seller" false nil)] nil))))
               nil))
(defn parallel-after-interaction [include-ids]
  (if include-ids (create-protocol [(-->> 1 "a" "b")
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]
                                                    ])])
                  (create-protocol [(->interaction nil 1 "a" "b" false nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)])))

(def parallel-after-interactionControl
  (->interaction nil 1 "a" "b" false
                 (->parallel nil [(->interaction nil 2 "b" "a" false
                                                 (->interaction nil 3 "a" "b" false nil))
                                  (->interaction nil 4 "b" "a" false
                                                 (->interaction nil 5 "a" "b" false nil))
                                  ] nil)))

(defn parallel-after-interaction-with-after [include-ids]
  (if include-ids (create-protocol [(make-interaction 1 "a" "b")
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]
                                                    ])
                                    (make-interaction 6 "b" "a")])
                  (create-protocol [(->interaction nil 1 "a" "b"  false nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)
                                    (->interaction nil 6 "b" "a" false nil)])))

(def parallel-after-interaction-with-afterControl
  (->interaction nil 1 "a" "b" false
                 (->parallel nil [(->interaction nil 2 "b" "a" false
                                                 (->interaction nil 3 "a" "b" false nil))
                                  (->interaction nil 4 "b" "a" false
                                                 (->interaction nil 5 "a" "b" false nil))
                                  ] (->interaction nil 6 "b" "a" false nil))))

(defn parallel-after-choice-with-after [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction 1 "a" "b")]
                                                  [(make-interaction 0 "a" "b")]])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-interaction 6 "b" "a")])
                  (create-protocol [(->branch nil [[(->interaction nil 1 "a" "b" false nil)]
                                                   [(->interaction nil 0 "a" "b" false nil)]] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)
                                    (->interaction nil 6 "b" "a" false nil)])))

(def parallel-after-choice-with-afterControl
  (->branch nil [(->interaction nil 1 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                              (->interaction nil 3 "a" "b" false nil))
                                                               (->interaction nil 4 "b" "a" false
                                                                              (->interaction nil 5 "a" "b" false nil))
                                                               ] (->interaction nil 6 "b" "a" false nil)))
                 (->interaction nil 0 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                              (->interaction nil 3 "a" "b" false nil))
                                                               (->interaction nil 4 "b" "a" false
                                                                              (->interaction nil 5 "a" "b" false nil))
                                                               ] (->interaction nil 6 "b" "a" false nil)))] nil))

(defn parallel-after-choice-with-after-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction 1 "a" "b")]
                                                  [(make-interaction 0 "a" "b")]])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-choice [[(make-interaction 6 "b" "a")]
                                                  [(make-interaction 7 "b" "a")]])])
                  (create-protocol [(->branch nil [[(->interaction nil 1 "a" "b" false nil)]
                                                   [(->interaction nil 0 "a" "b" false nil)]] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)
                                    (->branch nil [[(->interaction nil 6 "b" "a" false nil)]
                                                   [(->interaction nil 7 "b" "a" false nil)]] nil)])))
(def parallel-after-choice-with-after-choiceControl
  (->branch nil [(->interaction nil 1 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                              (->interaction nil 3 "a" "b" false nil))
                                                               (->interaction nil 4 "b" "a" false
                                                                              (->interaction nil 5 "a" "b" false nil))
                                                               ] (->branch nil [(->interaction nil 6 "b" "a" false nil)
                                                                                (->interaction nil 7 "b" "a" false nil)] nil)))
                 (->interaction nil 0 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                              (->interaction nil 3 "a" "b" false nil))
                                                               (->interaction nil 4 "b" "a" false
                                                                              (->interaction nil 5 "a" "b" false nil))
                                                               ] (->branch nil [(->interaction nil 6 "b" "a" false nil)
                                                                                (->interaction nil 7 "b" "a" false nil)] nil)))] nil))

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
                                                 [(->branch nil [[(->interaction nil 1 "a" "b" false nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil 0 "a" "b" false nil)]] nil)] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)
                                    (->branch nil [[(->interaction nil 6 "b" "a" false nil)]
                                                   [(->interaction nil 7 "b" "a" false nil)]] nil)])))
(def parallel-after-rec-with-afterControl
  (->recursion nil :test (->branch nil [(->interaction nil 1 "a" "b" false (->recur-identifier nil :test :recur nil))
                                        (->interaction nil 0 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                                                     (->interaction nil 3 "a" "b" false nil))
                                                                                      (->interaction nil 4 "b" "a" false
                                                                                                     (->interaction nil 5 "a" "b" false nil))
                                                                                      ] (->branch nil [(->interaction nil 6 "b" "a" false nil)
                                                                                                       (->interaction nil 7 "b" "a" false nil)] nil)))] nil) nil))

(defn parallel-after-rec-with-after-rec [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction 1 "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction 0 "a" "b")]])])
                                    (make-parallel [[(make-interaction 2 "b" "a")
                                                     (make-interaction 3 "a" "b")]
                                                    [(make-interaction 4 "b" "a")
                                                     (make-interaction 5 "a" "b")]])
                                    (make-recursion :test2 [
                                                            (make-choice [[(make-interaction 6 "b" "a")
                                                                           (do-recur :test2)]
                                                                          [(make-interaction 7 "b" "a")]])])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil 1 "a" "b" false nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil 0 "a" "b" false nil)]] nil)] nil)
                                    (->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                      (->interaction nil 3 "a" "b" false nil)]
                                                     [(->interaction nil 4 "b" "a" false nil)
                                                      (->interaction nil 5 "a" "b" false nil)]
                                                     ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil 6 "b" "a" false nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil 7 "b" "a" false nil)]] nil)] nil)])))

(def parallel-after-rec-with-after-recControl
  (->recursion nil :test (->branch nil [(->interaction nil 1 "a" "b" false (->recur-identifier nil :test :recur nil))
                                        (->interaction nil 0 "a" "b" false (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                                                     (->interaction nil 3 "a" "b" false nil))
                                                                                      (->interaction nil 4 "b" "a" false
                                                                                                     (->interaction nil 5 "a" "b" false nil))
                                                                                      ] (->recursion nil :test2
                                                                                                     (->branch nil [(->interaction nil 6 "b" "a" false
                                                                                                                                   (->recur-identifier nil :test2 :recur nil))
                                                                                                                    (->interaction nil 7 "b" "a" false nil)] nil) nil)))] nil) nil))
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
                  (create-protocol [(->interaction nil 1 "a" "b" false nil)
                                    (->parallel nil [[(->parallel nil [[(->interaction nil "a" "b" "a" false nil)
                                                                        (->interaction nil "b" "a" "b" false nil)]
                                                                       [(->interaction nil "b" "b" "a" false nil)
                                                                        (->interaction nil "a" "a" "b" false nil)]] nil)]
                                                     [(->parallel nil [[(->interaction nil 2 "b" "a" false nil)
                                                                        (->interaction nil 3 "a" "b" false nil)]
                                                                       [(->interaction nil 4 "b" "a" false nil)
                                                                        (->interaction nil 5 "a" "b" false nil)]] nil)]] nil)])))

(def nested-parallelControl (->interaction nil 1 "a" "b" false
                                           (->parallel nil [(->parallel nil [(->interaction nil "a" "b" "a" false
                                                                                            (->interaction nil "b" "a" "b" false nil))
                                                                             (->interaction nil "b" "b" "a" false
                                                                                            (->interaction nil "a" "a" "b" false nil))] nil)
                                                            (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                                            (->interaction nil 3 "a" "b" false nil))
                                                                             (->interaction nil 4 "b" "a" false
                                                                                            (->interaction nil 5 "a" "b" false nil))] nil)] nil)))
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
                  (create-protocol [(->parallel nil [[(->interaction nil 0 "b" "a" false nil)
                                                      (->interaction nil 1 "a" "b" false nil)]
                                                     [(->interaction nil "hi" "b" "a" false nil)
                                                      (->interaction nil "hi" "a" "b" false nil)]] nil)
                                    (->parallel nil [[(->parallel nil [[(->interaction nil "a" "b" "a" false nil)
                                                                        (->interaction nil "b" "a" "b" false nil)]
                                                                       [(->interaction nil "b" "b" "a" false nil)
                                                                        (->interaction nil "a" "a" "b" false nil)]] nil)]
                                                     [(->parallel nil [[(->interaction nil 2 "b" "a"false nil)
                                                                        (->interaction nil 3 "a" "b"false nil)]
                                                                       [(->interaction nil 4 "b" "a"false nil)
                                                                        (->interaction nil 5 "a" "b"false nil)]] nil)]] nil)])))
(def after-parallel-nested-parallelControl (->parallel nil [(->interaction nil 0 "b" "a" false
                                                                           (->interaction nil 1 "a" "b" false nil))
                                                            (->interaction nil "hi" "b" "a" false
                                                                           (->interaction nil "hi" "a" "b" false nil))]
                                                       (->parallel nil [(->parallel nil [(->interaction nil "a" "b" "a" false
                                                                                                        (->interaction nil "b" "a" "b" false nil))
                                                                                         (->interaction nil "b" "b" "a" false
                                                                                                        (->interaction nil "a" "a" "b" false nil))] nil)
                                                                        (->parallel nil [(->interaction nil 2 "b" "a" false
                                                                                                        (->interaction nil 3 "a" "b" false nil))
                                                                                         (->interaction nil 4 "b" "a" false
                                                                                                        (->interaction nil 5 "a" "b" false nil))] nil)] nil)))