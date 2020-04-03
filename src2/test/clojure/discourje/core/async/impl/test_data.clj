(ns discourje.core.async.impl.test-data
  (:require [clojure.test :refer :all]
            [discourje.core.async.impl.dsl.abstract :refer :all]
            [discourje.core.async.impl.dsl.concrete :refer :all]))

;legacy message usage, just to make the tests pass
(defprotocol message-sendable
  (get-label [this])
  (get-content [this]))

(defrecord message [label content]
  message-sendable
  (get-label [this] label)
  (get-content [this] content))

(defmacro msg
  "Generate a message"
  [label content]
  `(->message ~label ~content))

(defmacro message-checker [value]
  `(fn [~'m] (= (if (satisfies? message-sendable ~'m) (get-label ~'m) ~'m) ~value)))
;;------------------------------------------------------

(deftest interactableTest
  (let [inter (make-interaction (message-checker "1") "A" "B")]
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleMulticastProtocol []
  (create-protocol [(make-interaction (message-checker "1") "A" ["B" "C"])]))

(def testSingleMulticastProtocolControl
  [(->Interaction (uuid) "1" "A" ["B" "C"] #{} nil)])

(defn testDualProtocol [include-ids]
  (if include-ids
    (create-protocol [(make-interaction (message-checker "1") "A" "B")
                      (make-interaction (message-checker "2") "B" "A")])
    (create-protocol [(->Interaction nil nil "A" "B" #{} nil)
                      (->Interaction nil nil "B" "A" #{} nil)])))

(def testDualProtocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{} nil)))

(defn test-typed-DualProtocol [include-ids]
  (if include-ids (create-protocol [(-->> String "A" "B")
                                    (-->> String "B" "A")])
                  (create-protocol [(->Interaction nil nil "A" "B" #{} nil)
                                    (->Interaction nil nil "B" "A" #{} nil)])))
(def test-typed-DualProtocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{} nil)))

(defn testTripleProtocol [include-ids]
  (if include-ids
    (create-protocol [
                      (make-interaction (message-checker "1") "A" "B")
                      (make-interaction (message-checker "2") "B" "A")
                      (make-interaction (message-checker "3") "A" "C")]))
  (create-protocol [
                    (->Interaction nil nil "A" "B" #{} nil)
                    (->Interaction nil nil "B" "A" #{} nil)
                    (->Interaction nil nil "A" "C" #{} nil)]))

(def testTripleProtocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{}
                                (->Interaction nil nil "A" "C" #{} nil))))

(defn testMulticastProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (message-checker "1") "A" "B")
                                    (make-interaction (message-checker "2") "B" "A")
                                    (make-interaction (message-checker "3") "A" "C")
                                    (make-interaction (message-checker "4") "C" ["A" "B"])])
                  (create-protocol [
                                    (->Interaction nil nil "A" "B" #{} nil)
                                    (->Interaction nil nil "B" "A" #{} nil)
                                    (->Interaction nil nil "A" "C" #{} nil)
                                    (->Interaction nil nil "C" ["A" "B"] #{} nil)])))
(def testMulticastProtocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{}
                                (->Interaction nil nil "A" "C" #{}
                                               (->Interaction nil nil "C" ["A" "B"] #{} nil)))))

(defn testQuadProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (message-checker "start") "main" ["A" "B" "C"])
                                    (make-interaction (message-checker "1") "A" "B")
                                    (make-interaction (message-checker "2") "B" "A")
                                    (make-interaction (message-checker "3") "A" "C")
                                    (make-interaction (message-checker "4") "C" ["A" "B"])])
                  (create-protocol [
                                    (->Interaction nil nil "main" ["A" "B" "C"] #{} nil)
                                    (->Interaction nil nil "A" "B" #{} nil)
                                    (->Interaction nil nil "B" "A" #{} nil)
                                    (->Interaction nil nil "A" "C" #{} nil)
                                    (->Interaction nil nil "C" ["A" "B"] #{} nil)])))
(def testQuadProtocolControl
  (->Interaction nil nil "main" ["A" "B" "C"] #{}
                 (->Interaction nil nil "A" "B" #{}
                                (->Interaction nil nil "B" "A" #{}
                                               (->Interaction nil nil "A" "C" #{}
                                                              (->Interaction nil nil "C" ["A" "B"] #{} nil))))))

(defn testMulticastParticipantsProtocol []
  (mep (-->> (message-checker "1") "A" ["B" "C"])
       (-->> (message-checker "2") "B" "A")))

(defn testMulticastParticipantsWithChoiceProtocol []
  (mep (-->> (message-checker "1") "A" "B")
       (-->> (message-checker "2") "B" "A")
       (choice
         [(-->> (message-checker "3") "A" ["B" "C"])]
         [(-->> (message-checker "5") "A" ["B" "C"])])
       (-->> (message-checker "4") "B" "A")))

(defn single-choice-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction (message-checker "1") "A" "B")]
                                  [(make-interaction (message-checker "hi") "A" "C")]]
                                 )]))

(defn single-choice-in-middle-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker "99") "Start" "Finish")
                                    (make-choice [
                                                  [(make-interaction (message-checker "1") "A" "B")
                                                   (make-interaction (message-checker "bla") "B" "A")]
                                                  [(make-interaction (message-checker "2") "A" "C")
                                                   (make-interaction (message-checker "hello") "C" "A")]]
                                                 )
                                    (make-interaction (message-checker "88") "Finish" "Start")])
                  (create-protocol [(->Interaction nil nil "Start" "Finish" #{} nil)
                                    (->Choice nil [
                                                   [(->Interaction nil nil "A" "B" #{} nil)
                                                    (->Interaction nil nil "B" "A" #{} nil)]
                                                   [(->Interaction nil nil "A" "C" #{} nil)
                                                    (->Interaction nil nil "C" "A" #{} nil)]]
                                              nil)
                                    (->Interaction nil nil "Finish" "Start" #{} nil)])))
(def single-choice-in-middle-protocolControl
  (->Interaction nil nil "Start" "Finish" #{}
                 (->Choice nil [
                                (->Interaction nil nil "A" "B" #{}
                                               (->Interaction nil nil "B" "A" #{}
                                                              (->Interaction nil nil "Finish" "Start" #{} nil)))
                                (->Interaction nil nil "A" "C" #{}
                                               (->Interaction nil nil "C" "A" #{}
                                                              (->Interaction nil nil "Finish" "Start" #{} nil)))]
                           nil)))

(defn single-choice-5branches-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (message-checker "1") "A" "B")]
                                                  [(make-interaction (message-checker "1") "A" "C")]
                                                  [(make-interaction (message-checker "1") "A" "D")]
                                                  [(make-interaction (message-checker "1") "A" "E")]
                                                  [(make-interaction (message-checker "1") "A" "F")]
                                                  ]
                                                 )
                                    (make-interaction (message-checker "Done") "A" "End")])
                  (create-protocol [(->Choice nil [
                                                   [(->Interaction nil nil "A" "B" #{} nil)]
                                                   [(->Interaction nil nil "A" "C" #{} nil)]
                                                   [(->Interaction nil nil "A" "D" #{} nil)]
                                                   [(->Interaction nil nil "A" "E" #{} nil)]
                                                   [(->Interaction nil nil "A" "F" #{} nil)]
                                                   ]
                                              nil)
                                    (->Interaction nil nil "A" "End" #{} nil)])))
(def single-choice-5branches-protocolControl
  (->Choice nil [
                 (->Interaction nil nil "A" "B" #{} (->Interaction nil nil "A" "End" #{} nil))
                 (->Interaction nil nil "A" "C" #{} (->Interaction nil nil "A" "End" #{} nil))
                 (->Interaction nil nil "A" "D" #{} (->Interaction nil nil "A" "End" #{} nil))
                 (->Interaction nil nil "A" "E" #{} (->Interaction nil nil "A" "End" #{} nil))
                 (->Interaction nil nil "A" "F" #{} (->Interaction nil nil "A" "End" #{} nil))
                 ]
            nil))

(defn dual-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (message-checker "1") "A" "B")]
                                                  [(make-interaction (message-checker "hi") "A" "C")
                                                   (make-choice [
                                                                 [(make-interaction (message-checker "hiA") "C" "A")]
                                                                 [(make-interaction (message-checker "hiD") "C" "D")]]
                                                                )]]
                                                 )
                                    (make-interaction (message-checker "Done") "A" "End")]))
  (create-protocol [(->Choice nil [
                                   [(->Interaction nil nil "A" "B" #{} nil)]
                                   [(->Interaction nil nil "A" "C" #{} nil)
                                    (->Choice nil [
                                                   [(->Interaction nil nil "C" "A" #{} nil)]
                                                   [(->Interaction nil nil "C" "D" #{} nil)]]
                                              nil)]]
                              nil)
                    (->Interaction nil nil "A" "End" #{} nil)]))

(def dual-choice-protocolControl
  (->Choice nil [
                 (->Interaction nil nil "A" "B" #{} (->Interaction nil nil "A" "End" #{} nil))
                 (->Interaction nil nil "A" "C" #{}
                                (->Choice nil [
                                               (->Interaction nil nil "C" "A" #{} (->Interaction nil nil "A" "End" #{} nil))
                                               (->Interaction nil nil "C" "D" #{} (->Interaction nil nil "A" "End" #{} nil))]
                                          nil))]
            nil))

(defn single-choice-multiple-interactions-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker "1") "A" "B")
                                    (make-interaction (message-checker "1") "B" "A")
                                    (make-choice [
                                                  [(make-interaction (message-checker "2") "A" "C")
                                                   (make-interaction (message-checker "2") "C" "A")
                                                   (make-interaction (message-checker "3") "A" "C")
                                                   (make-interaction (message-checker "3") "C" "A")]
                                                  [(make-interaction (message-checker "2") "A" "B")
                                                   (make-interaction (message-checker "2") "B" "A")
                                                   (make-interaction (message-checker "3") "A" "B")
                                                   (make-interaction (message-checker "3") "B" "A")]])
                                    (make-interaction (message-checker "4") "A" "D")
                                    (make-interaction (message-checker "4") "D" "A")
                                    (make-interaction (message-checker "5") "A" ["B" "C" "D"])
                                    ])
                  (create-protocol [(->Interaction nil nil "A" "B" #{} nil)
                                    (->Interaction nil nil "B" "A" #{} nil)
                                    (->Choice nil [
                                                   [(->Interaction nil nil "A" "C" #{} nil)
                                                    (->Interaction nil nil "C" "A" #{} nil)
                                                    (->Interaction nil nil "A" "C" #{} nil)
                                                    (->Interaction nil nil "C" "A" #{} nil)]
                                                   [(->Interaction nil nil "A" "B" #{} nil)
                                                    (->Interaction nil nil "B" "A" #{} nil)
                                                    (->Interaction nil nil "A" "B" #{} nil)
                                                    (->Interaction nil nil "B" "A" #{} nil)]] nil)
                                    (->Interaction nil nil "A" "D" #{} nil)
                                    (->Interaction nil nil "D" "A" #{} nil)
                                    (->Interaction nil nil "A" ["B" "C" "D"] #{} nil)
                                    ])))
(def single-choice-multiple-interactions-protocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{}
                                (->Choice nil [
                                               (->Interaction nil nil "A" "C" #{}
                                                              (->Interaction nil nil "C" "A" #{}
                                                                             (->Interaction nil nil "A" "C" #{}
                                                                                            (->Interaction nil nil "C" "A" #{} (->Interaction nil nil "A" "D" #{}
                                                                                                                                              (->Interaction nil nil "D" "A" #{}
                                                                                                                                                             (->Interaction nil nil "A" ["B" "C" "D"] #{} nil)))))))
                                               (->Interaction nil nil "A" "B" #{}
                                                              (->Interaction nil nil "B" "A" #{}
                                                                             (->Interaction nil nil "A" "B" #{}
                                                                                            (->Interaction nil nil "B" "A" #{} (->Interaction nil nil "A" "D" #{}
                                                                                                                                              (->Interaction nil nil "D" "A" #{}
                                                                                                                                                             (->Interaction nil nil "A" ["B" "C" "D"] #{} nil)))))))] nil)
                                )))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction (message-checker "1") "A" "B")]
                                  [(make-choice [
                                                 [(make-interaction (message-checker "1") "A" "C")]
                                                 [(make-interaction (message-checker "1") "A" "D")]]
                                                )]]
                                 )
                    (make-interaction (message-checker "Done") "A" "End")]))

(defn multiple-nested-choice-branch-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-choice [
                                                                 [(make-interaction (message-checker "1") "A" "B")]
                                                                 [(make-interaction (message-checker "2") "A" "B")]]
                                                                )]
                                                  [(make-choice [
                                                                 [(make-interaction (message-checker "3") "A" "B")]
                                                                 [(make-interaction (message-checker "4") "A" "B")]]
                                                                )]]
                                                 )])
                  (create-protocol [(->Choice nil [
                                                   [(->Choice nil [
                                                                   [(->Interaction nil nil "A" "B" #{} nil)]
                                                                   [(->Interaction nil nil "A" "B" #{} nil)]]
                                                              nil)]
                                                   [(->Choice nil [
                                                                   [(->Interaction nil nil "A" "B" #{} nil)]
                                                                   [(->Interaction nil nil "A" "B" #{} nil)]]
                                                              nil)]]
                                              nil)])))
(def multiple-nested-choice-branch-protocolControl
  (->Choice nil [
                 (->Choice nil [
                                (->Interaction nil nil "A" "B" #{} nil)
                                (->Interaction nil nil "A" "B" #{} nil)]
                           nil)
                 (->Choice nil [
                                (->Interaction nil nil "A" "B" #{} nil)
                                (->Interaction nil nil "A" "B" #{} nil)]
                           nil)]
            nil))

(defn multiple-nested-branches-protocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-choice [
                                                  [(make-choice [
                                                                 [(make-interaction (message-checker "1") "A" "B")
                                                                  (make-interaction (message-checker "2") "B" "A")]
                                                                 [(make-interaction (message-checker "1") "A" "C")]]
                                                                )]
                                                  [(make-choice [
                                                                 [(make-choice [
                                                                                [(make-choice [
                                                                                               [(make-interaction (message-checker "1") "A" "D")]
                                                                                               [(make-interaction (message-checker "1") "A" ["E" "F" "G"])
                                                                                                (make-interaction (message-checker "3") "F" "A")
                                                                                                (make-interaction (message-checker "4") "G" "A")]]
                                                                                              )]
                                                                                [(make-interaction (message-checker "1") "A" "H")]]
                                                                               )]
                                                                 [(make-interaction (message-checker "1") "A" "I")]]
                                                                )]]
                                                 )
                                    (make-interaction (message-checker "Done") "A" "End")]
                                   )
                  (create-protocol [
                                    (->Choice nil [
                                                   [(->Choice nil [
                                                                   [(->Interaction nil nil "A" "B" #{} nil)
                                                                    (->Interaction nil nil "B" "A" #{} nil)]
                                                                   [(->Interaction nil nil "A" "C" #{} nil)]]
                                                              nil)]
                                                   [(->Choice nil [
                                                                   [(->Choice nil [
                                                                                   [(->Choice nil [
                                                                                                   [(->Interaction nil nil "A" "D" #{} nil)]
                                                                                                   [(->Interaction nil nil "A" ["E" "F" "G"] #{} nil)
                                                                                                    (->Interaction nil nil "F" "A" #{} nil)
                                                                                                    (->Interaction nil nil "G" "A" #{} nil)]]
                                                                                              nil)]
                                                                                   [(->Interaction nil nil "A" "H" #{} nil)]]
                                                                              nil)]
                                                                   [(->Interaction nil nil "A" "I" #{} nil)]]
                                                              nil)]]
                                              nil)
                                    (->Interaction nil nil "A" "End" #{} nil)]
                                   )))

(def multiple-nested-branches-protocolControl
  (->Choice nil [
                 (->Choice nil [
                                (->Interaction nil nil "A" "B" #{}
                                               (->Interaction nil nil "B" "A" #{} (->Interaction nil nil "A" "End" #{} nil)))
                                (->Interaction nil nil "A" "C" #{} (->Interaction nil nil "A" "End" #{} nil))]
                           nil)
                 (->Choice nil [
                                (->Choice nil [
                                               (->Choice nil [
                                                              (->Interaction nil nil "A" "D" #{} (->Interaction nil nil "A" "End" #{} nil))
                                                              (->Interaction nil nil "A" ["E" "F" "G"] #{}
                                                                             (->Interaction nil nil "F" "A" #{}
                                                                                            (->Interaction nil nil "G" "A" #{} (->Interaction nil nil "A" "End" #{} nil))))]
                                                         nil)
                                               (->Interaction nil nil "A" "H" #{} (->Interaction nil nil "A" "End" #{} nil))]
                                          nil)
                                (->Interaction nil nil "A" "I" #{} (->Interaction nil nil "A" "End" #{} nil))]
                           nil)]
            nil)
  )


(defn single-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker "1") "A" "B")
                                    (make-recursion :test [
                                                           (make-interaction (message-checker "1") "B" "A")
                                                           (make-choice [
                                                                         [(make-interaction (message-checker "2") "A" "C")
                                                                          (make-interaction (message-checker "2") "C" "A")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (message-checker "3") "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    (make-interaction (message-checker "end") "A" ["B" "C"])
                                    ])
                  (create-protocol [(->Interaction nil nil "A" "B" #{} nil)
                                    (->Recursion nil :test [
                                                            (->Interaction nil nil "B" "A" #{} nil)
                                                            (->Choice nil [
                                                                           [(->Interaction nil nil "A" "C" #{} nil)
                                                                            (->Interaction nil nil "C" "A" #{} nil)
                                                                            (->Continue nil :test :recur nil)]
                                                                           [(->Interaction nil nil "A" "B" #{} nil)]] nil)
                                                            ] nil)
                                    (->Interaction nil nil "A" ["B" "C"] #{} nil)
                                    ])))
(def single-recur-protocolControl
  (->Interaction nil nil "A" "B" #{}
                 (->Interaction nil nil "B" "A" #{}
                                (->Choice nil [
                                               (->Interaction nil nil "A" "C" #{}
                                                              (->Interaction nil nil "C" "A" #{}
                                                                             (->Continue nil :test :recur nil)))
                                               (->Interaction nil nil "A" "B" #{} (->Interaction nil nil "A" ["B" "C"] #{} nil))

                                               ] nil))
                 )
  )

(defn single-recur-one-choice-protocol []
  (create-protocol [(make-recursion :generate [
                                               (make-interaction (message-checker "1") "A" "B")
                                               (make-choice [
                                                             [(make-interaction (message-checker "2") "B" "A")
                                                              (do-recur :generate)]
                                                             [(make-interaction (message-checker "3") "B" "A")]
                                                             ])
                                               ])
                    ]))

(defn one-recur-with-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [
                                                                         [(make-interaction (message-checker "2") "A" "C")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (message-checker "3") "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    ])
                  (create-protocol [(->Recursion nil :test [
                                                            (->Choice nil [
                                                                           [(->Interaction nil nil "A" "C" #{} nil)
                                                                            (->Continue nil :test :recur nil)]
                                                                           [(->Interaction nil nil "A" "B" #{} nil)
                                                                            ]
                                                                           ] nil)
                                                            ] nil)
                                    ])
                  ))
(def one-recur-with-choice-protocolControl
  (->Choice nil [
                 (->Interaction nil nil "A" "C" #{}
                                (->Continue nil :test :recur nil))
                 (->Interaction nil nil "A" "B" #{} nil)
                 ] nil))

(defn one-recur-with-startchoice-and-endchoice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-recursion :test [
                                                                          (make-choice [
                                                                                        [(make-interaction (message-checker "2") "A" "C")
                                                                                         (do-recur :test)]
                                                                                        [(make-interaction (message-checker "3") "A" "B")
                                                                                         ]
                                                                                        ])
                                                                          ])
                                                   ]
                                                  [(make-interaction (message-checker "2") "A" "C")]
                                                  ])
                                    ])
                  (create-protocol [(->Choice nil [
                                                   [(->Recursion nil :test [
                                                                            (->Choice nil [
                                                                                           [(->Interaction nil nil "A" "C" #{} nil)
                                                                                            (->Continue nil :test :recur nil)]
                                                                                           [(->Interaction nil nil "A" "B" #{} nil)
                                                                                            ]
                                                                                           ] nil)
                                                                            ] nil)
                                                    ]
                                                   [(->Interaction nil nil "A" "C" #{} nil)]
                                                   ] nil)
                                    ])))

(def one-recur-with-startchoice-and-endchoice-protocolControl
  (->Choice nil [(->Choice nil [(->Interaction nil nil "A" "C" #{}
                                               (->Continue nil :test :recur nil))
                                (->Interaction nil nil "A" "B" #{} nil)
                                ] nil)
                 (->Interaction nil nil "A" "C" #{} nil)
                 ] nil))


(defn nested-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-recursion :nested [
                                                                                    (make-interaction (message-checker "1") "B" "A")
                                                                                    (make-choice [
                                                                                                  [(make-interaction (message-checker "2") "A" "C")
                                                                                                   (make-interaction (message-checker "2") "C" "A")
                                                                                                   (do-recur :nested)]
                                                                                                  [(make-interaction (message-checker "3") "A" "B")]
                                                                                                  ])
                                                                                    (make-choice [
                                                                                                  [(make-interaction (message-checker "2") "A" "C")
                                                                                                   (make-interaction (message-checker "2") "C" "D")
                                                                                                   (do-recur :test)]
                                                                                                  [(make-interaction (message-checker "3") "A" "E")]
                                                                                                  ])
                                                                                    ])]

                                                    )
                                    (make-interaction (message-checker "end") "A" ["B" "C"])
                                    ])
                  (create-protocol [(->Recursion nil :test [
                                                            (->Recursion nil :nested [
                                                                                      (->Interaction nil nil "B" "A" #{} nil)
                                                                                      (->Choice nil [
                                                                                                     [(->Interaction nil nil "A" "C" #{} nil)
                                                                                                      (->Interaction nil nil "C" "A" #{} nil)
                                                                                                      (->Continue nil :nested :recur nil)]
                                                                                                     [(->Interaction nil nil "A" "B" #{} nil)]
                                                                                                     ] nil)
                                                                                      (->Choice nil [
                                                                                                     [(->Interaction nil nil "A" "C" #{} nil)
                                                                                                      (->Interaction nil nil "C" "D" #{} nil)
                                                                                                      (->Continue nil :test :recur nil)]
                                                                                                     [(->Interaction nil nil "A" "E" #{} nil)]
                                                                                                     ] nil)
                                                                                      ] nil)]

                                                 nil)
                                    (->Interaction nil nil "A" ["B" "C"] #{} nil)])))
(def nested-recur-protocolControl
  (->Interaction nil nil "B" "A" #{}
                 (->Choice nil [
                                (->Interaction nil nil "A" "C" #{}
                                               (->Interaction nil nil "C" "A" #{}
                                                              (->Continue nil :nested :recur nil)))
                                (->Interaction nil nil "A" "B" #{} (->Choice nil [
                                                                                  (->Interaction nil nil "A" "C" #{}
                                                                                                 (->Interaction nil nil "C" "D" #{}
                                                                                                                (->Continue nil :test :recur nil)))
                                                                                  (->Interaction nil nil "A" "E" #{} (->Interaction nil nil "A" ["B" "C"] #{} nil))
                                                                                  ] nil))
                                ] nil))
  )

(defn multiple-nested-recur-protocol []
  (create-protocol [(make-recursion :test [
                                           (make-recursion :nested [
                                                                    (make-choice [
                                                                                  [(make-recursion :nested-again [
                                                                                                                  (make-interaction (message-checker "2") "A" "C")
                                                                                                                  (make-interaction (message-checker "2") "C" "A")
                                                                                                                  (do-recur :nested-again)])]
                                                                                  [(make-interaction (message-checker "4") "A" "B")
                                                                                   (do-recur :nested)]
                                                                                  [(make-interaction (message-checker "3") "A" "D")]
                                                                                  ])
                                                                    (make-choice [
                                                                                  [(make-interaction (message-checker "2") "A" "C")
                                                                                   (make-interaction (message-checker "2") "C" "E")
                                                                                   (do-recur :test)]
                                                                                  [(make-interaction (message-checker "3") "A" "F")]
                                                                                  ])
                                                                    ])]

                                    )
                    ]))

(defn two-buyer-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :order-book [
                                                                 (make-interaction (message-checker "title") "Buyer1" "Seller")
                                                                 (make-interaction (message-checker "quote") "Seller" ["Buyer1" "Buyer2"])
                                                                 (make-interaction (message-checker "quoteDiv") "Buyer1" "Buyer2")
                                                                 (make-choice [
                                                                               [(make-interaction (message-checker "ok") "Buyer2" "Seller")
                                                                                (make-interaction (message-checker "date") "Seller" "Buyer2")
                                                                                (do-recur :order-book)]
                                                                               [(make-interaction (message-checker "quit") "Buyer2" "Seller")]])])
                                    ])
                  (create-protocol [(->Recursion nil :order-book [
                                                                  (->Interaction nil nil "Buyer1" "Seller" #{} nil)
                                                                  (->Interaction nil nil "Seller" ["Buyer1" "Buyer2"] #{} nil)
                                                                  (->Interaction nil nil "Buyer1" "Buyer2" #{} nil)
                                                                  (->Choice nil [
                                                                                 [(->Interaction nil nil "Buyer2" "Seller" #{} nil)
                                                                                  (->Interaction nil nil "Seller" "Buyer2" #{} nil)
                                                                                  (->Continue nil :order-book :recur nil)]
                                                                                 [(->Interaction nil nil "Buyer2" "Seller" #{} nil)]] nil)] nil)
                                    ])))
(def two-buyer-protocolControl
  (->Interaction nil nil "Buyer1" "Seller" #{}
                 (->Interaction nil nil "Seller" ["Buyer1" "Buyer2"] #{}
                                (->Interaction nil nil "Buyer1" "Buyer2" #{}
                                               (->Choice nil [
                                                              (->Interaction nil nil "Buyer2" "Seller" #{}
                                                                             (->Interaction nil nil "Seller" "Buyer2" #{}
                                                                                            (->Continue nil :order-book :recur nil)))
                                                              (->Interaction nil nil "Buyer2" "Seller" #{} nil)] nil)))))
(defn parallel-after-interaction [include-ids]
  (if include-ids (create-protocol [(-->> (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)])))

(def parallel-after-interactionControl
  (->Interaction nil nil "a" "b" #{}
                 (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                 (->Interaction nil nil "a" "b" #{} nil))
                                 (->Interaction nil nil "b" "a" #{}
                                                (->Interaction nil nil "a" "b" #{} nil))
                                 ] nil)))

(defn parallel-after-interaction-with-after [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(def parallel-after-interaction-with-afterControl
  (->Interaction nil nil "a" "b" #{}
                 (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                 (->Interaction nil nil "a" "b" #{} nil))
                                 (->Interaction nil nil "b" "a" #{}
                                                (->Interaction nil nil "a" "b" #{} nil))
                                 ] (->Interaction nil nil "b" "a" #{} nil))))

(defn parallel-after-choice-with-after [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                  [(make-interaction (message-checker 0) "a" "b")]])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                   [(->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(def parallel-after-choice-with-afterControl
  (->Choice nil [(->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ] (->Interaction nil nil "b" "a" #{} nil)))
                 (->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ] (->Interaction nil nil "b" "a" #{} nil)))] nil))

(defn parallel-after-choice-with-after-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                  [(make-interaction (message-checker 0) "a" "b")]])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-choice [[(make-interaction (message-checker 6) "b" "a")]
                                                  [(make-interaction (message-checker 7) "b" "a")]])])
                  (create-protocol [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                   [(->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Choice nil [[(->Interaction nil nil "b" "a" #{} nil)]
                                                   [(->Interaction nil nil "b" "a" #{} nil)]] nil)])))
(def parallel-after-choice-with-after-choiceControl
  (->Choice nil [(->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ] (->Choice nil [(->Interaction nil nil "b" "a" #{} nil)
                                                                                     (->Interaction nil nil "b" "a" #{} nil)] nil)))
                 (->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ] (->Choice nil [(->Interaction nil nil "b" "a" #{} nil)
                                                                                     (->Interaction nil nil "b" "a" #{} nil)] nil)))] nil))

(defn parallel-after-rec-with-after [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (message-checker 1) "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (message-checker 0) "a" "b")]])])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-choice [[(make-interaction (message-checker 6) "b" "a")]
                                                  [(make-interaction (message-checker 7) "b" "a")]])])
                  (create-protocol [(->Recursion nil :test
                                                 [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)
                                                                  (->Continue nil :test :recur nil)]
                                                                 [(->Interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Choice nil [[(->Interaction nil nil "b" "a" #{} nil)]
                                                   [(->Interaction nil nil "b" "a" #{} nil)]] nil)])))
(def parallel-after-rec-with-afterControl
  (->Choice nil [(->Interaction nil nil "a" "b" #{} (->Continue nil :test :recur nil))
                 (->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ] (->Choice nil [(->Interaction nil nil "b" "a" #{} nil)
                                                                                     (->Interaction nil nil "b" "a" #{} nil)] nil)))] nil))

(defn parallel-after-rec-with-after-rec [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (message-checker 1) "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (message-checker 0) "a" "b")]])])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-recursion :test2 [
                                                            (make-choice [[(make-interaction (message-checker 6) "b" "a")
                                                                           (do-recur :test2)]
                                                                          [(make-interaction (message-checker 7) "b" "a")]])])])
                  (create-protocol [(->Recursion nil :test
                                                 [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)
                                                                  (->Continue nil :test :recur nil)]
                                                                 [(->Interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Recursion nil :test2
                                                 [(->Choice nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                  (->Continue nil :test2 :recur nil)]
                                                                 [(->Interaction nil nil "b" "a" #{} nil)]] nil)] nil)])))

(def parallel-after-rec-with-after-recControl
  (->Choice nil [(->Interaction nil nil "a" "b" #{} (->Continue nil :test :recur nil))
                 (->Interaction nil nil "a" "b" #{} (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                    (->Interaction nil nil "a" "b" #{} nil))
                                                                    (->Interaction nil nil "b" "a" #{}
                                                                                   (->Interaction nil nil "a" "b" #{} nil))
                                                                    ]
                                                                (->Choice nil [(->Interaction nil nil "b" "a" #{}
                                                                                              (->Continue nil :test2 :recur nil))
                                                                              (->Interaction nil nil "b" "a" #{} nil)] nil)))] nil))
(defn nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 1) "a" "b")
                                    (make-parallel [[(make-parallel [[(make-interaction (message-checker "a") "b" "a")
                                                                      (make-interaction (message-checker "b") "a" "b")]
                                                                     [(make-interaction (message-checker "b") "b" "a")
                                                                      (make-interaction (message-checker "a") "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                                      (make-interaction (message-checker 3) "a" "b")]
                                                                     [(make-interaction (message-checker 4) "b" "a")
                                                                      (make-interaction (message-checker 5) "a" "b")]])]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]] nil)])))

(def nested-parallelControl (->Interaction nil nil "a" "b" #{}
                                           (->Parallel nil [(->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                            (->Interaction nil nil "a" "b" #{} nil))
                                                                           (->Interaction nil nil "b" "a" #{}
                                                                                          (->Interaction nil nil "a" "b" #{} nil))] nil)
                                                           (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                           (->Interaction nil nil "a" "b" #{} nil))
                                                                           (->Interaction nil nil "b" "a" #{}
                                                                                          (->Interaction nil nil "a" "b" #{} nil))] nil)] nil)))
(defn after-parallel-nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-interaction (message-checker 0) "b" "a")
                                                     (make-interaction (message-checker 1) "a" "b")]
                                                    [(make-interaction (message-checker "hi") "b" "a")
                                                     (make-interaction (message-checker "hi") "a" "b")]])
                                    (make-parallel [[(make-parallel [[(make-interaction (message-checker "a") "b" "a")
                                                                      (make-interaction (message-checker "b") "a" "b")]
                                                                     [(make-interaction (message-checker "b") "b" "a")
                                                                      (make-interaction (message-checker "a") "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                                      (make-interaction (message-checker 3) "a" "b")]
                                                                     [(make-interaction (message-checker 4) "b" "a")
                                                                      (make-interaction (message-checker 5) "a" "b")]])]])])
                  (create-protocol [(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Parallel nil [[(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]] nil)])))
(def after-parallel-nested-parallelControl (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                           (->Interaction nil nil "a" "b" #{} nil))
                                                           (->Interaction nil nil "b" "a" #{}
                                                                          (->Interaction nil nil "a" "b" #{} nil))]
                                                       (->Parallel nil [(->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                                        (->Interaction nil nil "a" "b" #{} nil))
                                                                                      (->Interaction nil nil "b" "a" #{}
                                                                                                     (->Interaction nil nil "a" "b" #{} nil))] nil)
                                                                      (->Parallel nil [(->Interaction nil nil "b" "a" #{}
                                                                                                      (->Interaction nil nil "a" "b" #{} nil))
                                                                                      (->Interaction nil nil "b" "a" #{}
                                                                                                     (->Interaction nil nil "a" "b" #{} nil))] nil)] nil)))

(defn parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                   [(make-interaction (message-checker 0) "a" "b")]])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                                    [(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-choice-control
  (->Parallel nil [(->Choice nil [(->Interaction nil nil "a" "b" #{} nil)
                                 (->Interaction nil nil "a" "b" #{} nil)] nil)
                  (->Interaction nil nil "b" "a" #{} (->Interaction nil nil "a" "b" #{} nil))
                  ]
              (->Interaction nil nil "b" "a" #{} nil)))

(defn parallel-with-choice-with-parallel [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [
                                                                   [(make-parallel [
                                                                                    [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                                                   [(make-interaction (message-checker 0) "a" "b")]])]
                                                                                    [(make-interaction (message-checker 4) "b" "a")
                                                                                     (make-interaction (message-checker 5) "a" "b")]])]
                                                                   [(make-interaction (message-checker 9) "a" "b")]])]
                                                    [(make-interaction (message-checker "hi") "b" "a")
                                                     (make-interaction (message-checker "hi") "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Parallel nil [[(->Choice nil [[(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                                                                     [(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                    [(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(defn parallel-with-choice-with-parallelMulticast [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [
                                                                   [(make-parallel [
                                                                                    [(make-choice [[(make-interaction (message-checker 1) "a" ["b" "c"])]
                                                                                                   [(make-interaction (message-checker 0) "a" ["b" "c"])]])]
                                                                                    [(make-interaction (message-checker 4) "b" ["a" "c"])
                                                                                     (make-interaction (message-checker 5) "a" ["b" "c"])]])]
                                                                   [(make-interaction (message-checker 9) "a" ["b" "c"])]])]
                                                    [(make-interaction (message-checker "hi") "b" ["a" "c"])
                                                     (make-interaction (message-checker "hi") "a" ["b" "c"])]])
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])
                  (create-protocol [(->Parallel nil [[(->Choice nil [[(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                                                                     [(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                    [(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-choice-with-parallel-control
  (->Parallel nil [(->Choice nil [(->Parallel nil [(->Choice nil [(->Interaction nil nil "a" "b" #{} nil)
                                                                (->Interaction nil nil "a" "b" #{} nil)] nil)
                                                 (->Interaction nil nil "b" "a" #{} (->Interaction nil nil "a" "b" #{} nil))
                                                 ] nil)
                                 (->Interaction nil nil "a" "b" #{} nil)] nil)
                  (->Interaction nil nil "b" "a" #{} (->Interaction nil nil "a" "b" #{} nil))
                  ] (->Interaction nil nil "b" "a" #{} nil)))

(defn parallel-with-rec [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-recursion :test [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                                          [(make-interaction (message-checker 0) "a" "b")
                                                                                           (do-recur :test)]])
                                                                            ])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Parallel nil [[(->Recursion nil :test [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                                                            [(->Interaction nil nil "a" "b" #{} nil)
                                                                                             (->Continue nil :test :recur nil)]] nil)] nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-rec-control
  (->Parallel nil [(->Choice nil [(->Interaction nil nil "a" "b" #{} nil)
                                 (->Interaction nil nil "a" "b" #{}
                                                (->Continue nil :test :recur nil))] nil)
                  (->Interaction nil nil "b" "a" #{} (->Interaction nil nil "a" "b" #{} nil))]
              (->Interaction nil nil "b" "a" #{} nil)))

(defn rec-with-parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                                          [(make-interaction (message-checker 0) "a" "b")
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (message-checker 4) "b" "a")
                                                                            (make-interaction (message-checker 5) "a" "b")]
                                                                           ])
                                                           ])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->Recursion nil :test [(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]
                                                                                            [(->Interaction nil nil "a" "b" #{} nil)
                                                                                             (->Continue nil :test :recur nil)]] nil)]
                                                                            [(->Interaction nil nil "b" "a" #{} nil)
                                                                             (->Interaction nil nil "a" "b" #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->Interaction nil nil "b" "a" #{} nil)])))

(defn rec-with-parallel-with-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" ["b" "c"])]
                                                                                          [(make-interaction (message-checker 0) "a" ["b" "c"])
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (message-checker 4) "b" ["a" "c"])
                                                                            (make-interaction (message-checker 5) "a" ["b" "c"])]
                                                                           ])
                                                           ])
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])
                  (create-protocol [(->Recursion nil :test [(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                                            [(->Interaction nil nil "a" ["b" "c"] #{} nil)
                                                                                             (->Continue nil :test :recur nil)]] nil)]
                                                                            [(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                                             (->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->Interaction nil nil "b" ["a" "c"] #{} nil)])))

(defn multiple-branches-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (message-checker 0) "a" "b")
                                                   (make-interaction (message-checker 1) "b" "a")]
                                                  [(make-interaction (message-checker 2) "a" "b")
                                                   (make-interaction (message-checker 3) "b" "a")]
                                                  [(make-interaction (message-checker 4) "a" "b")
                                                   (make-interaction (message-checker 5) "b" "a")]
                                                  ])
                                    ])
                  (create-protocol [(->Choice nil [
                                                   [(->Interaction nil nil "a" "b" #{} nil)
                                                    (->Interaction nil nil "b" "a" #{} nil)]
                                                   [(->Interaction nil nil "a" "b" #{} nil)
                                                    (->Interaction nil nil "b" "a" #{} nil)]
                                                   [(->Interaction nil nil "a" "b" #{} nil)
                                                    (->Interaction nil nil "b" "a" #{} nil)]
                                                   ] nil)])))

(defn parallel-after-interaction-multicast [include-ids]
  (if include-ids (create-protocol [(-->> (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" ["a" "c"])
                                                     (make-interaction (message-checker 3) "a" ["b" "c"])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])))

(def parallel-after-interaction-multicastControl
  (->Interaction nil nil "a" "b" #{}
                 (->Parallel nil [(->Interaction nil nil "b" ["a" "c"] #{}
                                                 (->Interaction nil nil "a" ["b" "c"] #{} nil))
                                 (->Interaction nil nil "b" "a" #{}
                                                (->Interaction nil nil "a" "b" #{} nil))
                                 ] (->Interaction nil nil "b" ["a" "c"] #{} nil))))

(defn parallel-after-choice-with-after-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" ["a" "c"])
                                                     (make-interaction (message-checker 3) "a" ["b" "c"])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-choice [[(make-interaction (message-checker 6) "b" ["a" "c"])]
                                                  [(make-interaction (message-checker 7) "b" "a")]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Choice nil [[(->Interaction nil nil "b" ["a" "c"] #{} nil)]
                                                   [(->Interaction nil nil "b" "a" #{} nil)]] nil)])))

(defn parallel-after-rec-with-after-rec-multicasts [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (message-checker 1) "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (message-checker 0) "a" "b")]])])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" ["a" "c"])
                                                     (make-interaction (message-checker 3) "a" ["b" "c"])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-recursion :test2 [
                                                            (make-choice [[(make-interaction (message-checker 6) "b" ["a" "c"])
                                                                           (do-recur :test2)]
                                                                          [(make-interaction (message-checker 7) "b" "a")]])])])
                  (create-protocol [(->Recursion nil :test
                                                 [(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)
                                                                  (->Continue nil :test :recur nil)]
                                                                 [(->Interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->Parallel nil [[(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->Recursion nil :test2
                                                 [(->Choice nil [[(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                                  (->Continue nil :test2 :recur nil)]
                                                                 [(->Interaction nil nil "b" "a" #{} nil)]] nil)] nil)])))

(defn rec-with-parallel-with-choice-multicast-and-close [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" ["b" "c"])]
                                                                                          [(make-interaction (message-checker 0) "a" ["b" "c"])
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (message-checker 4) "b" ["a" "c"])
                                                                            (make-interaction (message-checker 5) "a" ["b" "c"])]
                                                                           ])
                                                           ])
                                    (make-closer "a" "b")
                                    (make-closer "a" "c")
                                    (make-interaction (message-checker 6) "b" ["a" "c"])
                                    (make-closer "b" "a")
                                    (make-closer "b" "c")])
                  (create-protocol [(->Recursion nil :test [(->Parallel nil [[(->Choice nil [[(->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                                            [(->Interaction nil nil "a" ["b" "c"] #{} nil)
                                                                                             (->Continue nil :test :recur nil)]] nil)]
                                                                            [(->Interaction nil nil "b" ["a" "c"] #{} nil)
                                                                             (->Interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->Close nil "a" "b" nil)
                                    (->Close nil "a" "c" nil)
                                    (->Interaction nil nil "b" ["a" "c"] #{} nil)
                                    (->Close nil "b" "a" nil)
                                    (->Close nil "b" "c" nil)])))


(def rec-with-parallel-with-choice-multicast-and-closeControl
  (->Parallel nil [(->Choice nil [(->Interaction nil nil "a" ["b" "c"] #{} nil)
                                 (->Interaction nil nil "a" ["b" "c"] #{}
                                                (->Continue nil :test :recur nil))] nil)
                  (->Interaction nil nil "b" ["a" "c"] #{}
                                 (->Interaction nil nil "a" ["b" "c"] #{} nil))
                  ]
              (->Close nil "a" "b"
                       (->Close nil "a" "c"
                                (->Interaction nil nil "b" ["a" "c"] #{}
                                               (->Close nil "b" "a"
                                                        (->Close nil "b" "c" nil)))))))

(defn interaction-with-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-closer "a" "b")])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Close nil "a" "b" nil)])))

(defn interaction-with-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-choice [[
                                                   (make-closer "a" "b")]
                                                  [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Choice nil [[(->Close nil "a" "b" nil)]
                                                   [(->Interaction nil nil "a" "b" #{} nil)]] nil)])))

(defn interaction-with-rec-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-recursion :test [
                                                           (make-closer "a" "b")])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Recursion nil :test [(->Close nil "a" "b" nil)] nil)])))

(defn interaction-with-rec-and-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-recursion :test
                                                    [(make-choice [[(make-closer "a" "b")]
                                                                   [(make-interaction (message-checker 1) "a" "b")
                                                                    (do-recur :test)]])])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Recursion nil :test
                                                 [(->Choice nil [[(->Close nil "a" "b" nil)]
                                                                 [(->Interaction nil nil "a" "b" #{} nil)
                                                                  (do-recur :test)]] nil)] nil)])))

(defn interaction-with-parallel-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-parallel [[(make-closer "a" "b")]
                                                    [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Close nil "a" "b" nil)]
                                                    [(->Interaction nil nil "a" "b" #{} nil)]] nil)])))
(defn interaction-with-parallel-and-closer-with-interactions-in-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-parallel [[(make-closer "a" "b")
                                                     (make-interaction (message-checker 2) "b" "a")
                                                     (make-closer "b" "a")]
                                                    [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Parallel nil [[(->Close nil "a" "b" nil)]
                                                    [(->Interaction nil nil "a" "b" #{} nil)]] nil)])))

(defn interaction-with-nested-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-choice [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]])]
                                                  [(make-choice [[(make-closer "a" "b")]])]])])
                  (create-protocol [(->Interaction nil nil "a" "b" #{} nil)
                                    (->Choice nil [[(->Choice nil [[(->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                   [(->Choice nil [[(->Close nil "a" "b" nil)]] nil)]] nil)])))

(defn after-parallel-nested-parallel-with-closer [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-interaction (message-checker 0) "b" "a")
                                                     (make-interaction (message-checker 1) "a" "b")]
                                                    [(make-interaction (message-checker "hi") "b" "a")
                                                     (make-interaction (message-checker "hi") "a" "b")]])
                                    (make-parallel [[(make-parallel [[(make-interaction (message-checker "a") "b" "a")
                                                                      (make-interaction (message-checker "b") "a" "b")]
                                                                     [(make-interaction (message-checker "b") "b" "a")
                                                                      (make-interaction (message-checker "a") "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                                      (make-interaction (message-checker 3) "a" "b")]
                                                                     [(make-closer "a" "b")
                                                                      (make-closer "b" "a")]])]])
                                    ])
                  (create-protocol [(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]
                                                    [(->Interaction nil nil "b" "a" #{} nil)
                                                     (->Interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->Parallel nil [[(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->Parallel nil [[(->Interaction nil nil "b" "a" #{} nil)
                                                                      (->Interaction nil nil "a" "b" #{} nil)]
                                                                     [(->Close nil "a" "b" nil)
                                                                      (->Close nil "b" "a" nil)]] nil)]] nil)])))