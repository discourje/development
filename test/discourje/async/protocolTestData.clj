(ns discourje.async.protocolTestData
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [clj-uuid :as uuid]))

;legacy message usage, just to make the tests pass
(defprotocol sendable
  (get-label [this])
  (get-content [this]))

(defrecord message [label content]
  sendable
  (get-label [this] label)
  (get-content [this] content))

(defmacro msg
  "Generate a message"
  [label content]
  `(->message ~label ~content))

(defmacro message-checker [value]
  `(fn [~'m] (= (if (satisfies? sendable ~'m) (get-label ~'m) ~'m) ~value)))
;;------------------------------------------------------

(deftest interactableTest
  (let [inter (make-interaction (message-checker "1") "A" "B")]
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleMulticastProtocol []
  (create-protocol [(make-interaction (message-checker "1") "A" ["B" "C"])]))

(def testSingleMulticastProtocolControl
  [(->interaction (uuid/v1) "1" "A" ["B" "C"] #{} nil)])

(defn testDualProtocol [include-ids]
  (if include-ids
    (create-protocol [(make-interaction (message-checker "1") "A" "B")
                      (make-interaction (message-checker "2") "B" "A")]))
  (create-protocol [(->interaction nil nil "A" "B" #{} nil)
                    (->interaction nil nil "B" "A" #{} nil)]))

(def testDualProtocolControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{} nil)))

(defn test-typed-DualProtocol [include-ids]
  (when include-ids (create-protocol [(make-interaction (message-checker java.lang.String) "A" "B")
                                      (make-interaction (message-checker java.lang.String) "B" "A")])
                    (create-protocol [(->interaction nil nil "A" "B" #{} nil)
                                      (->interaction nil nil "B" "A" #{} nil)])))
(def test-typed-DualProtocolControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{} nil)))

(defn testTripleProtocol [include-ids]
  (if include-ids
    (create-protocol [
                      (make-interaction (message-checker "1") "A" "B")
                      (make-interaction (message-checker "2") "B" "A")
                      (make-interaction (message-checker "3") "A" "C")]))
  (create-protocol [
                    (->interaction nil nil "A" "B" #{} nil)
                    (->interaction nil nil "B" "A" #{} nil)
                    (->interaction nil nil "A" "C" #{} nil)]))

(def testTripleProtocolControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{}
                                (->interaction nil nil "A" "C" #{} nil))))

(defn testMulticastProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (message-checker "1") "A" "B")
                                    (make-interaction (message-checker "2") "B" "A")
                                    (make-interaction (message-checker "3") "A" "C")
                                    (make-interaction (message-checker "4") "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil nil "A" "B" #{} nil)
                                    (->interaction nil nil "B" "A" #{} nil)
                                    (->interaction nil nil "A" "C" #{} nil)
                                    (->interaction nil nil "C" ["A" "B"] #{} nil)])))
(def testMulticastProtocolControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{}
                                (->interaction nil nil "A" "C" #{}
                                               (->interaction nil nil "C" ["A" "B"] #{} nil)))))

(defn testQuadProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (message-checker "start") "main" ["A" "B" "C"])
                                    (make-interaction (message-checker "1") "A" "B")
                                    (make-interaction (message-checker "2") "B" "A")
                                    (make-interaction (message-checker "3") "A" "C")
                                    (make-interaction (message-checker "4") "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil nil "main" ["A" "B" "C"] #{} nil)
                                    (->interaction nil nil "A" "B" #{} nil)
                                    (->interaction nil nil "B" "A" #{} nil)
                                    (->interaction nil nil "A" "C" #{} nil)
                                    (->interaction nil nil "C" ["A" "B"] #{} nil)])))
(def testQuadProtocolControl
  (->interaction nil nil "main" ["A" "B" "C"] #{}
                 (->interaction nil nil "A" "B" #{}
                                (->interaction nil nil "B" "A" #{}
                                               (->interaction nil nil "A" "C" #{}
                                                              (->interaction nil nil "C" ["A" "B"] #{} nil))))))

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
                  (create-protocol [(->interaction nil nil "Start" "Finish" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil nil "A" "B" #{} nil)
                                                    (->interaction nil nil "B" "A" #{} nil)]
                                                   [(->interaction nil nil "A" "C" #{} nil)
                                                    (->interaction nil nil "C" "A" #{} nil)]]
                                              nil)
                                    (->interaction nil nil "Finish" "Start" #{} nil)])))
(def single-choice-in-middle-protocolControl
  (->interaction nil nil "Start" "Finish" #{}
                 (->branch nil [
                                (->interaction nil nil "A" "B" #{}
                                               (->interaction nil nil "B" "A" #{}
                                                              (->interaction nil nil "Finish" "Start" #{} nil)))
                                (->interaction nil nil "A" "C" #{}
                                               (->interaction nil nil  "C" "A" #{}
                                                              (->interaction nil nil "Finish" "Start" #{} nil)))]
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
                  (create-protocol [(->branch nil [
                                                   [(->interaction nil nil "A" "B" #{} nil)]
                                                   [(->interaction nil nil "A" "C" #{} nil)]
                                                   [(->interaction nil nil "A" "D" #{} nil)]
                                                   [(->interaction nil nil "A" "E" #{} nil)]
                                                   [(->interaction nil nil "A" "F" #{} nil)]
                                                   ]
                                              nil)
                                    (->interaction nil nil  "A" "End" #{} nil)])))
(def single-choice-5branches-protocolControl
  (->branch nil [
                 (->interaction nil nil "A" "B" #{} (->interaction nil nil "A" "End" #{} nil))
                 (->interaction nil nil "A" "C" #{} (->interaction nil nil "A" "End" #{} nil))
                 (->interaction nil nil "A" "D" #{} (->interaction nil nil "A" "End" #{} nil))
                 (->interaction nil nil "A" "E" #{} (->interaction nil nil "A" "End" #{} nil))
                 (->interaction nil nil "A" "F" #{} (->interaction nil nil "A" "End" #{} nil))
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
  (create-protocol [(->branch nil [
                                   [(->interaction nil nil  "A" "B" #{} nil)]
                                   [(->interaction nil nil "A" "C" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil nil  "C" "A" #{} nil)]
                                                   [(->interaction nil nil  "C" "D" #{} nil)]]
                                              nil)]]
                              nil)
                    (->interaction nil nil "A" "End" #{} nil)]))

(def dual-choice-protocolControl
  (->branch nil [
                 (->interaction nil nil "A" "B" #{} (->interaction nil nil "A" "End" #{} nil))
                 (->interaction nil nil "A" "C" #{}
                                (->branch nil [
                                               (->interaction nil nil "C" "A" #{} (->interaction nil nil "A" "End" #{} nil))
                                               (->interaction nil nil "C" "D" #{} (->interaction nil nil "A" "End" #{} nil))]
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
                  (create-protocol [(->interaction nil nil "A" "B" #{} nil)
                                    (->interaction nil nil "B" "A" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil nil  "A" "C" #{} nil)
                                                    (->interaction nil nil  "C" "A" #{} nil)
                                                    (->interaction nil nil  "A" "C" #{} nil)
                                                    (->interaction nil nil  "C" "A" #{} nil)]
                                                   [(->interaction nil nil  "A" "B" #{} nil)
                                                    (->interaction nil nil "B" "A" #{} nil)
                                                    (->interaction nil nil "A" "B" #{} nil)
                                                    (->interaction nil nil "B" "A" #{} nil)]] nil)
                                    (->interaction nil nil "A" "D" #{} nil)
                                    (->interaction nil nil "D" "A" #{} nil)
                                    (->interaction nil nil "A" ["B" "C" "D"] #{} nil)
                                    ])))
(def single-choice-multiple-interactions-protocolControl
  (->interaction nil nil  "A" "B" #{}
                 (->interaction nil nil "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil nil "A" "C" #{}
                                                              (->interaction nil nil "C" "A" #{}
                                                                             (->interaction nil nil "A" "C" #{}
                                                                                            (->interaction nil nil "C" "A" #{} (->interaction nil nil  "A" "D" #{}
                                                                                                                                                                (->interaction nil nil  "D" "A" #{}
                                                                                                                                                                               (->interaction nil nil  "A" ["B" "C" "D"] #{} nil)))))))
                                               (->interaction nil nil "A" "B" #{}
                                                              (->interaction nil nil  "B" "A" #{}
                                                                             (->interaction nil nil  "A" "B" #{}
                                                                                            (->interaction nil nil "B" "A" #{} (->interaction nil nil  "A" "D" #{}
                                                                                                                                                                (->interaction nil nil "D" "A" #{}
                                                                                                                                                                               (->interaction nil nil "A" ["B" "C" "D"] #{} nil)))))))] nil)
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
                  (create-protocol [(->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil nil "A" "B" #{} nil)]
                                                                   [(->interaction nil nil "A" "B" #{} nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->interaction nil nil "A" "B" #{} nil)]
                                                                   [(->interaction nil nil "A" "B" #{} nil)]]
                                                              nil)]]
                                              nil)])))
(def multiple-nested-choice-branch-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil nil "A" "B" #{} nil)
                                (->interaction nil nil "A" "B" #{} nil)]
                           nil)
                 (->branch nil [
                                (->interaction nil nil "A" "B" #{} nil)
                                (->interaction nil nil "A" "B" #{} nil)]
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
                                    (->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil nil "A" "B" #{} nil)
                                                                    (->interaction nil nil "B" "A" #{} nil)]
                                                                   [(->interaction nil nil "A" "C" #{} nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->branch nil [
                                                                                   [(->branch nil [
                                                                                                   [(->interaction nil nil "A" "D" #{} nil)]
                                                                                                   [(->interaction nil nil "A" ["E" "F" "G"] #{} nil)
                                                                                                    (->interaction nil nil "F" "A" #{} nil)
                                                                                                    (->interaction nil nil  "G" "A" #{} nil)]]
                                                                                              nil)]
                                                                                   [(->interaction nil nil  "A" "H" #{} nil)]]
                                                                              nil)]
                                                                   [(->interaction nil nil "A" "I" #{} nil)]]
                                                              nil)]]
                                              nil)
                                    (->interaction nil nil "A" "End" #{} nil)]
                                   )))

(def multiple-nested-branches-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil nil "A" "B" #{}
                                               (->interaction nil nil "B" "A" #{} (->interaction nil nil  "A" "End" #{} nil)))
                                (->interaction nil nil "A" "C" #{} (->interaction nil nil  "A" "End" #{} nil))]
                           nil)
                 (->branch nil [
                                (->branch nil [
                                               (->branch nil [
                                                              (->interaction nil nil "A" "D" #{} (->interaction nil nil "A" "End" #{} nil))
                                                              (->interaction nil nil "A" ["E" "F" "G"] #{}
                                                                             (->interaction nil nil "F" "A" #{}
                                                                                            (->interaction nil nil "G" "A" #{} (->interaction nil nil "A" "End" #{} nil))))]
                                                         nil)
                                               (->interaction nil nil "A" "H" #{} (->interaction nil nil "A" "End" #{} nil))]
                                          nil)
                                (->interaction nil nil  "A" "I" #{} (->interaction nil nil  "A" "End" #{} nil))]
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
                  (create-protocol [(->interaction nil nil  "A" "B" #{} nil)
                                    (->recursion nil :test [
                                                            (->interaction nil nil "B" "A" #{} nil)
                                                            (->branch nil [
                                                                           [(->interaction nil nil "A" "C" #{} nil)
                                                                            (->interaction nil nil "C" "A" #{} nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil nil "A" "B" #{} nil)]] nil)
                                                            ] nil)
                                    (->interaction nil nil "A" ["B" "C"] #{} nil)
                                    ])))
(def single-recur-protocolControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil nil "A" "C" #{}
                                                              (->interaction nil nil  "C" "A" #{}
                                                                             (->recur-identifier nil :test :recur nil)))
                                               (->interaction nil nil "A" "B" #{} (->interaction nil nil  "A" ["B" "C"] #{} nil))

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
                  (create-protocol [(->recursion nil :test [
                                                            (->branch nil [
                                                                           [(->interaction nil nil  "A" "C" #{} nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil nil  "A" "B" #{} nil)
                                                                            ]
                                                                           ] nil)
                                                            ] nil)
                                    ])
                  ))
(def one-recur-with-choice-protocolControl
  (->branch nil [
                 (->interaction nil nil  "A" "C" #{}
                                (->recur-identifier nil :test :recur nil))
                 (->interaction nil nil  "A" "B" #{} nil)
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
                  (create-protocol [(->branch nil [
                                                   [(->recursion nil :test [
                                                                            (->branch nil [
                                                                                           [(->interaction nil nil  "A" "C" #{} nil)
                                                                                            (->recur-identifier nil :test :recur nil)]
                                                                                           [(->interaction nil nil  "A" "B" #{} nil)
                                                                                            ]
                                                                                           ] nil)
                                                                            ] nil)
                                                    ]
                                                   [(->interaction nil nil "A" "C" #{} nil)]
                                                   ] nil)
                                    ])))

(def one-recur-with-startchoice-and-endchoice-protocolControl
  (->branch nil [(->branch nil [(->interaction nil nil "A" "C" #{}
                                               (->recur-identifier nil :test :recur nil))
                                (->interaction nil nil  "A" "B" #{} nil)
                                ] nil)
                 (->interaction nil nil "A" "C" #{} nil)
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
                  (create-protocol [(->recursion nil :test [
                                                            (->recursion nil :nested [
                                                                                      (->interaction nil nil  "B" "A" #{} nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil nil  "A" "C" #{} nil)
                                                                                                      (->interaction nil nil  "C" "A" #{} nil)
                                                                                                      (->recur-identifier nil :nested :recur nil)]
                                                                                                     [(->interaction nil nil "A" "B" #{} nil)]
                                                                                                     ] nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil nil  "A" "C" #{} nil)
                                                                                                      (->interaction nil nil  "C" "D" #{} nil)
                                                                                                      (->recur-identifier nil :test :recur nil)]
                                                                                                     [(->interaction nil nil "A" "E" #{} nil)]
                                                                                                     ] nil)
                                                                                      ] nil)]

                                                 nil)
                                    (->interaction nil nil  "A" ["B" "C"] #{} nil)])))
(def nested-recur-protocolControl
  (->interaction nil nil  "B" "A" #{}
                 (->branch nil [
                                (->interaction nil nil "A" "C" #{}
                                               (->interaction nil nil  "C" "A" #{}
                                                              (->recur-identifier nil :nested :recur nil)))
                                (->interaction nil nil  "A" "B" #{} (->branch nil [
                                                                                                    (->interaction nil nil  "A" "C" #{}
                                                                                                                   (->interaction nil nil  "C" "D" #{}
                                                                                                                                  (->recur-identifier nil :test :recur nil)))
                                                                                                    (->interaction nil nil "A" "E" #{} (->interaction nil nil  "A" ["B" "C"] #{} nil))
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
                  (create-protocol [(->recursion nil :order-book [
                                                                  (->interaction nil nil "Buyer1" "Seller" #{} nil)
                                                                  (->interaction nil nil "Seller" ["Buyer1" "Buyer2"] #{} nil)
                                                                  (->interaction nil nil  "Buyer1" "Buyer2" #{} nil)
                                                                  (->branch nil [
                                                                                 [(->interaction nil nil "Buyer2" "Seller" #{} nil)
                                                                                  (->interaction nil nil "Seller" "Buyer2" #{} nil)
                                                                                  (->recur-identifier nil :order-book :recur nil)]
                                                                                 [(->interaction nil nil "Buyer2" "Seller" #{} nil)]] nil)] nil)
                                    ])))
(def two-buyer-protocolControl
  (->interaction nil nil  "Buyer1" "Seller" #{}
                 (->interaction nil nil  "Seller" ["Buyer1" "Buyer2"] #{}
                                (->interaction nil nil  "Buyer1" "Buyer2" #{}
                                               (->branch nil [
                                                              (->interaction nil nil  "Buyer2" "Seller" #{}
                                                                             (->interaction nil nil  "Seller" "Buyer2" #{}
                                                                                            (->recur-identifier nil :order-book :recur nil)))
                                                              (->interaction nil nil  "Buyer2" "Seller" #{} nil)] nil)))))
(defn parallel-after-interaction [include-ids]
  (if include-ids (create-protocol [(-->> (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)])))

(def parallel-after-interactionControl
  (->interaction nil nil "a" "b" #{}
                 (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                (->interaction nil nil "a" "b" #{} nil))
                                 (->interaction nil nil"b" "a" #{}
                                                (->interaction nil nil"a" "b" #{} nil))
                                 ] nil)))

(defn parallel-after-interaction-with-after [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    [(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    ] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(def parallel-after-interaction-with-afterControl
  (->interaction nil nil "a" "b" #{}
                 (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                (->interaction nil nil "a" "b" #{} nil))
                                 (->interaction nil nil  "b" "a" #{}
                                                (->interaction nil nil "a" "b" #{} nil))
                                 ] (->interaction nil nil  "b" "a" #{} nil))))

(defn parallel-after-choice-with-after [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                  [(make-interaction (message-checker 0) "a" "b")]])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->branch nil [[(->interaction nil nil  "a" "b" #{} nil)]
                                                   [(->interaction nil nil  "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(def parallel-after-choice-with-afterControl
  (->branch nil [(->interaction nil nil "a" "b" #{} (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ] (->interaction nil nil "b" "a" #{} nil)))
                 (->interaction nil nil "a" "b" #{} (->lateral nil [(->interaction nil nil"b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ] (->interaction nil nil "b" "a" #{} nil)))] nil))

(defn parallel-after-choice-with-after-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                  [(make-interaction (message-checker 0) "a" "b")]])
                                    (make-parallel [[(make-interaction (message-checker 2) "b" "a")
                                                     (make-interaction (message-checker 3) "a" "b")]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-choice [[(make-interaction (message-checker 6) "b" "a")]
                                                  [(make-interaction (message-checker 7) "b" "a")]])])
                  (create-protocol [(->branch nil [[(->interaction nil nil  "a" "b" #{} nil)]
                                                   [(->interaction nil nil  "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    [(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil nil  "b" "a" #{} nil)]
                                                   [(->interaction nil nil  "b" "a" #{} nil)]] nil)])))
(def parallel-after-choice-with-after-choiceControl
  (->branch nil [(->interaction nil nil "a" "b" #{} (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil"a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ] (->branch nil [(->interaction nil nil  "b" "a" #{} nil)
                                                                                                     (->interaction nil nil "b" "a" #{} nil)] nil)))
                 (->interaction nil nil "a" "b" #{} (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ] (->branch nil [(->interaction nil nil "b" "a" #{} nil)
                                                                                                     (->interaction nil nil "b" "a" #{} nil)] nil)))] nil))

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
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil nil "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    [(->interaction nil nil  "b" "a" #{} nil)
                                                     (->interaction nil nil  "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil nil "b" "a" #{} nil)]
                                                   [(->interaction nil nil "b" "a" #{} nil)]] nil)])))
(def parallel-after-rec-with-afterControl
  (->branch nil [(->interaction nil nil  "a" "b" #{} (->recur-identifier nil :test :recur nil))
                 (->interaction nil nil  "a" "b" #{} (->lateral nil [(->interaction nil nil"b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ] (->branch nil [(->interaction nil nil "b" "a" #{} nil)
                                                                                                     (->interaction nil nil "b" "a" #{} nil)] nil)))] nil))

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
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil nil "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil nil "b" "a" #{} nil)]] nil)] nil)])))

(def parallel-after-rec-with-after-recControl
  (->branch nil [(->interaction nil nil "a" "b" #{} (->recur-identifier nil :test :recur nil))
                 (->interaction nil nil "a" "b" #{} (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    (->interaction nil nil "b" "a" #{}
                                                                                                   (->interaction nil nil "a" "b" #{} nil))
                                                                                    ]
                                                                               (->branch nil [(->interaction nil nil "b" "a" #{}
                                                                                                             (->recur-identifier nil :test2 :recur nil))
                                                                                              (->interaction nil nil "b" "a" #{} nil)] nil)))] nil))
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
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]
                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]
                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]] nil)])))

(def nested-parallelControl (->interaction nil nil "a" "b" #{}
                                           (->lateral nil [(->lateral nil [(->interaction nil nil  "b" "a" #{}
                                                                                          (->interaction nil nil  "a" "b" #{} nil))
                                                                           (->interaction nil nil "b" "a" #{}
                                                                                          (->interaction nil nil "a" "b" #{} nil))] nil)
                                                           (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                          (->interaction nil nil "a" "b" #{} nil))
                                                                           (->interaction nil nil "b" "a" #{}
                                                                                          (->interaction nil nil "a" "b" #{} nil))] nil)] nil)))
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
                  (create-protocol [(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil nil  "b" "a" #{} nil)
                                                                      (->interaction nil nil  "a" "b" #{} nil)]
                                                                     [(->interaction nil nil  "b" "a" #{} nil)
                                                                      (->interaction nil nil  "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]
                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]] nil)])))
(def after-parallel-nested-parallelControl (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                          (->interaction nil nil "a" "b" #{} nil))
                                                           (->interaction nil nil  "b" "a" #{}
                                                                          (->interaction nil nil  "a" "b" #{} nil))]
                                                      (->lateral nil [(->lateral nil [(->interaction nil nil  "b" "a" #{}
                                                                                                     (->interaction nil nil  "a" "b" #{} nil))
                                                                                      (->interaction nil nil  "b" "a" #{}
                                                                                                     (->interaction nil nil "a" "b" #{} nil))] nil)
                                                                      (->lateral nil [(->interaction nil nil "b" "a" #{}
                                                                                                     (->interaction nil nil  "a" "b" #{} nil))
                                                                                      (->interaction nil nil "b" "a" #{}
                                                                                                     (->interaction nil nil  "a" "b" #{} nil))] nil)] nil)))

(defn parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                   [(make-interaction (message-checker 0) "a" "b")]])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->lateral nil [[(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]
                                                                    [(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-choice-control
  (->lateral nil [(->branch nil [(->interaction nil nil "a" "b" #{} nil)
                                 (->interaction nil nil "a" "b" #{} nil)] nil)
                  (->interaction nil nil "b" "a" #{} (->interaction nil nil "a" "b" #{} nil))
                  ]
             (->interaction nil nil "b" "a" #{} nil)))

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
                  (create-protocol [(->lateral nil [[(->branch nil [[(->lateral nil [[(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]
                                                                                                     [(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                    [(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

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
                  (create-protocol [(->lateral nil [[(->branch nil [[(->lateral nil [[(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]
                                                                                                     [(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                                    [(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-choice-with-parallel-control
  (->lateral nil [(->branch nil [(->lateral nil [(->branch nil [(->interaction nil nil "a" "b" #{} nil)
                                                                (->interaction nil nil "a" "b" #{} nil)] nil)
                                                 (->interaction nil nil "b" "a" #{} (->interaction nil nil "a" "b" #{} nil))
                                                 ] nil)
                                 (->interaction nil nil "a" "b" #{} nil)] nil)
                  (->interaction nil nil "b" "a" #{} (->interaction nil nil  "a" "b" #{} nil))
                  ] (->interaction nil nil "b" "a" #{} nil)))

(defn parallel-with-rec [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-recursion :test [(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                                          [(make-interaction (message-checker 0) "a" "b")
                                                                                           (do-recur :test)]])
                                                                            ])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->lateral nil [[(->recursion nil :test [(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]
                                                                                            [(->interaction nil nil "a" "b" #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)] nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(def parallel-with-rec-control
  (->lateral nil [(->branch nil [(->interaction nil nil "a" "b" #{} nil)
                                 (->interaction nil nil "a" "b" #{}
                                                (->recur-identifier nil :test :recur nil))] nil)
                  (->interaction nil nil "b" "a" #{} (->interaction nil nil "a" "b" #{} nil))]
             (->interaction nil nil "b" "a" #{} nil)))

(defn rec-with-parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]
                                                                                          [(make-interaction (message-checker 0) "a" "b")
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (message-checker 4) "b" "a")
                                                                            (make-interaction (message-checker 5) "a" "b")]
                                                                           ])
                                                           ])
                                    (make-interaction (message-checker 6) "b" "a")])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]
                                                                                            [(->interaction nil nil "a" "b" #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil nil "b" "a" #{} nil)
                                                                             (->interaction nil nil "a" "b" #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->interaction nil nil "b" "a" #{} nil)])))

(defn rec-with-parallel-with-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (message-checker 1) "a" ["b" "c"])]
                                                                                          [(make-interaction (message-checker 0) "a" ["b" "c"])
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (message-checker 4) "b" ["a" "c"])
                                                                            (make-interaction (message-checker 5) "a" ["b" "c"])]
                                                                           ])
                                                           ])
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                                            [(->interaction nil nil "a" ["b" "c"] #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                                             (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->interaction nil nil "b" ["a" "c"] #{} nil)])))

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
                  (create-protocol [(->branch nil [
                                                   [(->interaction nil nil "a" "b" #{} nil)
                                                    (->interaction nil nil "b" "a" #{} nil)]
                                                   [(->interaction nil nil "a" "b" #{} nil)
                                                    (->interaction nil nil "b" "a" #{} nil)]
                                                   [(->interaction nil nil "a" "b" #{} nil)
                                                    (->interaction nil nil "b" "a" #{} nil)]
                                                   ] nil)])))

(defn parallel-after-interaction-multicast [include-ids]
  (if include-ids (create-protocol [(-->> (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" ["a" "c"])
                                                     (make-interaction (message-checker 3) "a" ["b" "c"])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]
                                                    ])
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (make-interaction (message-checker 6) "b" ["a" "c"])])))

(def parallel-after-interaction-multicastControl
  (->interaction nil nil "a" "b" #{}
                 (->lateral nil [(->interaction nil nil "b" ["a" "c"] #{}
                                                (->interaction nil nil "a" ["b" "c"] #{} nil))
                                 (->interaction nil nil "b" "a" #{}
                                                (->interaction nil nil "a" "b" #{} nil))
                                 ] (->interaction nil nil "b" ["a" "c"] #{} nil))))

(defn parallel-after-choice-with-after-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 1) "a" "b")
                                    (make-parallel [[(make-interaction (message-checker 2) "b" ["a" "c"])
                                                     (make-interaction (message-checker 3) "a" ["b" "c"])]
                                                    [(make-interaction (message-checker 4) "b" "a")
                                                     (make-interaction (message-checker 5) "a" "b")]])
                                    (make-choice [[(make-interaction (message-checker 6) "b" ["a" "c"])]
                                                  [(make-interaction (message-checker 7) "b" "a")]])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil nil "b" ["a" "c"] #{} nil)]
                                                   [(->interaction nil nil "b" "a" #{} nil)]] nil)])))

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
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil nil "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil nil "a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                     (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil nil "b" "a" #{} nil)]] nil)] nil)])))

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
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                                            [(->interaction nil nil "a" ["b" "c"] #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil nil "b" ["a" "c"] #{} nil)
                                                                             (->interaction nil nil "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->closer nil "a" "b" nil)
                                    (->closer nil "a" "c" nil)
                                    (->interaction nil nil "b" ["a" "c"] #{} nil)
                                    (->closer nil "b" "a" nil)
                                    (->closer nil "b" "c" nil)])))

(def rec-with-parallel-with-choice-multicast-and-closeControl
  (->lateral nil [(->branch nil [(->interaction nil nil "a" ["b" "c"] #{} nil)
                                 (->interaction nil nil "a" ["b" "c"] #{}
                                                (->recur-identifier nil :test :recur nil))] nil)
                  (->interaction nil nil "b" ["a" "c"] #{}
                                 (->interaction nil nil "a" ["b" "c"] #{} nil))
                  ]
             (->closer nil "a" "b"
                       (->closer nil "a" "c"
                                 (->interaction nil nil "b" ["a" "c"] #{}
                                                (->closer nil "b" "a"
                                                          (->closer nil "b" "c" nil)))))))

(defn interaction-with-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-closer "a" "b")])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->closer nil "a" "b" nil)])))

(defn interaction-with-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-choice [[
                                                   (make-closer "a" "b")]
                                                  [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->branch nil [[(->closer nil "a" "b" nil)]
                                                   [(->interaction nil nil "a" "b" #{} nil)]] nil)])))

(defn interaction-with-rec-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-recursion :test [
                                                           (make-closer "a" "b")])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->recursion nil :test [(->closer nil "a" "b" nil)] nil)])))

(defn interaction-with-rec-and-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-recursion :test
                                                    [(make-choice [[(make-closer "a" "b")]
                                                                   [(make-interaction (message-checker 1) "a" "b")
                                                                    (do-recur :test)]])])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->recursion nil :test
                                                 [(->branch nil [[(->closer nil "a" "b" nil)]
                                                                 [(->interaction nil nil "a" "b" #{} nil)
                                                                  (do-recur :test)]] nil)] nil)])))

(defn interaction-with-parallel-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-parallel [[(make-closer "a" "b")]
                                                    [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->closer nil "a" "b" nil)]
                                                    [(->interaction nil nil "a" "b" #{} nil)]] nil)])))
(defn interaction-with-parallel-and-closer-with-interactions-in-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-parallel [[(make-closer "a" "b")
                                                     (make-interaction (message-checker 2) "b" "a")
                                                     (make-closer "b" "a")]
                                                    [(make-interaction (message-checker 1) "a" "b")]])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->lateral nil [[(->closer nil "a" "b" nil)]
                                                    [(->interaction nil nil "a" "b" #{} nil)]] nil)])))

(defn interaction-with-nested-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker 0) "a" "b")
                                    (make-choice [[(make-choice [[(make-interaction (message-checker 1) "a" "b")]])]
                                                  [(make-choice [[(make-closer "a" "b")]])]])])
                  (create-protocol [(->interaction nil nil "a" "b" #{} nil)
                                    (->branch nil [[(->branch nil [[(->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                   [(->branch nil [[(->closer nil "a" "b" nil)]] nil)]] nil)])))

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
                                                                      (make-closer "b" "a")]])]])])
                  (create-protocol [(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]
                                                    [(->interaction nil nil "b" "a" #{} nil)
                                                     (->interaction nil nil "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]
                                                                     [(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil nil "b" "a" #{} nil)
                                                                      (->interaction nil nil "a" "b" #{} nil)]
                                                                     [(->closer nil "a" "b" nil)
                                                                      (->closer nil "b" "a" nil)]] nil)]] nil)])))