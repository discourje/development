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
;;------------------------------------------------------

(deftest interactableTest
  (let [inter (make-interaction (fn [m] (= m "1" ))"A" "B")]
    (is (= "A" (get-sender inter)))
    (is (= "B" (get-receivers inter)))))

(defn testSingleMulticastProtocol []
  (create-protocol [(make-interaction (fn [m] (= m "1" ))"A" ["B" "C"])]))

(def testSingleMulticastProtocolControl
  [(->interaction (uuid/v1) "1" "A" ["B" "C"] #{} nil)])

(defn testDualProtocol [include-ids]
  (if include-ids
    (create-protocol [(make-interaction (fn [m] (= m "1"))"A" "B")
                      (make-interaction (fn [m] (= m "2")) "B" "A")]))
  (create-protocol [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                    (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)]))

(def testDualProtocolControl
  (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                 (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)))

(defn test-typed-DualProtocol [include-ids]
  (when include-ids (create-protocol [(make-interaction (fn [m] (= m java.lang.String))"A" "B")
                                      (make-interaction (fn [m] (= m java.lang.String)) "B" "A")])
                    (create-protocol [(->interaction nil (fn [m] (= m java.lang.String)) "A" "B" #{} nil)
                                      (->interaction nil (fn [m] (= m java.lang.String)) "B" "A" #{} nil)])))
(def test-typed-DualProtocolControl
  (->interaction nil (fn [m] (= m java.lang.String)) "A" "B" #{}
                 (->interaction nil (fn [m] (= m java.lang.String)) "B" "A" #{} nil)))

(defn testTripleProtocol [include-ids]
  (if include-ids
    (create-protocol [
                      (make-interaction (fn [m] (= m "1")) "A" "B")
                      (make-interaction (fn [m] (= m "2")) "B" "A")
                      (make-interaction (fn [m] (= m "3")) "A" "C")]))
  (create-protocol [
                    (->interaction nil (fn [m] (= m "1" ))"A" "B" #{} nil)
                    (->interaction nil (fn [m] (= m "2" ))"B" "A" #{} nil)
                    (->interaction nil (fn [m] (= m "3" ))"A" "C" #{} nil)]))

(def testTripleProtocolControl
  (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                 (->interaction nil (fn [m] (= m "2")) "B" "A" #{}
                                (->interaction nil (fn [m] (= m "3")) "A" "C" #{} nil))))

(defn testMulticastProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (fn [m] (= m "1")) "A" "B")
                                    (make-interaction (fn [m] (= m "2")) "B" "A")
                                    (make-interaction (fn [m] (= m "3")) "A" "C")
                                    (make-interaction (fn [m] (= m "4")) "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                    (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)
                                    (->interaction nil (fn [m] (= m "3")) "A" "C" #{} nil)
                                    (->interaction nil (fn [m] (= m "4")) "C" ["A" "B"] #{} nil)])))
(def testMulticastProtocolControl
  (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                 (->interaction nil (fn [m] (= m "2")) "B" "A" #{}
                                (->interaction nil (fn [m] (= m "3")) "A" "C" #{}
                                               (->interaction nil (fn [m] (= m "4")) "C" ["A" "B"] #{} nil)))))

(defn testQuadProtocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-interaction (fn [m] (= m "start")) "main" ["A" "B" "C"])
                                    (make-interaction (fn [m] (= m "1")) "A" "B")
                                    (make-interaction (fn [m] (= m "2")) "B" "A")
                                    (make-interaction (fn [m] (= m "3")) "A" "C")
                                    (make-interaction (fn [m] (= m "4")) "C" ["A" "B"])])
                  (create-protocol [
                                    (->interaction nil (fn [m] (= m "start")) "main" ["A" "B" "C"] #{} nil)
                                    (->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                    (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)
                                    (->interaction nil (fn [m] (= m "3")) "A" "C" #{} nil)
                                    (->interaction nil (fn [m] (= m "4")) "C" ["A" "B"] #{} nil)])))
(def testQuadProtocolControl
  (->interaction nil (fn [m] (= m "start")) "main" ["A" "B" "C"] #{}
                 (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                                (->interaction nil (fn [m] (= m "2")) "B" "A" #{}
                                               (->interaction nil (fn [m] (= m "3")) "A" "C" #{}
                                                              (->interaction nil (fn [m] (= m "4" ))"C" ["A" "B"] #{} nil))))))

(defn testMulticastParticipantsProtocol []
  (mep (-->> (fn [m] (= m "1")) "A" ["B" "C"])
       (-->> (fn [m] (= m "2")) "B" "A")))

(defn testMulticastParticipantsWithChoiceProtocol []
  (mep (-->> (fn [m] (= m "1")) "A" "B")
       (-->> (fn [m] (= m "2")) "B" "A")
       (choice
         [(-->> (fn [m] (= m "3")) "A" ["B" "C"])]
         [(-->> (fn [m] (= m "5")) "A" ["B" "C"])])
       (-->> (fn [m] (= m "4")) "B" "A")))

(defn single-choice-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction (fn [m] (= m "1")) "A" "B")]
                                  [(make-interaction (fn [m] (= m "hi" ))"A" "C")]]
                                 )]))

(defn single-choice-in-middle-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m "99")) "Start" "Finish")
                                    (make-choice [
                                                  [(make-interaction (fn [m] (= m "1")) "A" "B")
                                                   (make-interaction (fn [m] (= m "bla")) "B" "A")]
                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                   (make-interaction (fn [m] (= m "hello")) "C" "A")]]
                                                 )
                                    (make-interaction (fn [m] (= m "88")) "Finish" "Start")])
                  (create-protocol [(->interaction nil (fn [m] (= m "99")) "Start" "Finish" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                                    (->interaction nil (fn [m] (= m "bla")) "B" "A" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                    (->interaction nil (fn [m] (= m "hello")) "C" "A" #{} nil)]]
                                              nil)
                                    (->interaction nil (fn [m] (= m "88")) "Finish" "Start" #{} nil)])))
(def single-choice-in-middle-protocolControl
  (->interaction nil (fn [m] (= m "99")) "Start" "Finish" #{}
                 (->branch nil [
                                (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                                               (->interaction nil (fn [m] (= m "bla")) "B" "A" #{}
                                                              (->interaction nil (fn [m] (= m "88")) "Finish" "Start" #{} nil)))
                                (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                               (->interaction nil (fn [m] (= m "hello")) "C" "A" #{}
                                                              (->interaction nil (fn [m] (= m "88")) "Finish" "Start" #{} nil)))]
                           nil)))

(defn single-choice-5branches-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (fn [m] (= m "1")) "A" "B")]
                                                  [(make-interaction (fn [m] (= m "1")) "A" "C")]
                                                  [(make-interaction (fn [m] (= m "1")) "A" "D")]
                                                  [(make-interaction (fn [m] (= m "1")) "A" "E")]
                                                  [(make-interaction (fn [m] (= m "1")) "A" "F")]
                                                  ]
                                                 )
                                    (make-interaction (fn [m] (= m "Done")) "A" "End")])
                  (create-protocol [(->branch nil [
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "C" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "D" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "E" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "1")) "A" "F" #{} nil)]
                                                   ]
                                              nil)
                                    (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil)])))
(def single-choice-5branches-protocolControl
  (->branch nil [
                 (->interaction nil (fn [m] (= m "1")) "A" "B" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 (->interaction nil (fn [m] (= m "1")) "A" "C" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 (->interaction nil (fn [m] (= m "1")) "A" "D" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 (->interaction nil (fn [m] (= m "1")) "A" "E" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 (->interaction nil (fn [m] (= m "1")) "A" "F" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 ]
            nil))

(defn dual-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (fn [m] (= m "1")) "A" "B")]
                                                  [(make-interaction (fn [m] (= m "hi")) "A" "C")
                                                   (make-choice [
                                                                 [(make-interaction (fn [m] (= m "hiA")) "C" "A")]
                                                                 [(make-interaction (fn [m] (= m "hiD")) "C" "D")]]
                                                                )]]
                                                 )
                                    (make-interaction (fn [m] (= m "Done")) "A" "End")]))
  (create-protocol [(->branch nil [
                                   [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)]
                                   [(->interaction nil (fn [m] (= m "hi")) "A" "C" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil (fn [m] (= m "hiA")) "C" "A" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "hiD")) "C" "D" #{} nil)]]
                                              nil)]]
                              nil)
                    (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil)]))

(def dual-choice-protocolControl
  (->branch nil [
                 (->interaction nil (fn [m] (= m "1")) "A" "B" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                 (->interaction nil (fn [m] (= m "hi")) "A" "C" #{}
                                (->branch nil [
                                               (->interaction nil (fn [m] (= m "hiA")) "C" "A" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                                               (->interaction nil (fn [m] (= m "hiD")) "C" "D" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))]
                                          nil))]
            nil))

(defn single-choice-multiple-interactions-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m "1")) "A" "B")
                                    (make-interaction (fn [m] (= m "1")) "B" "A")
                                    (make-choice [
                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                   (make-interaction (fn [m] (= m "2")) "C" "A")
                                                   (make-interaction (fn [m] (= m "3")) "A" "C")
                                                   (make-interaction (fn [m] (= m "3")) "C" "A")]
                                                  [(make-interaction (fn [m] (= m "2")) "A" "B")
                                                   (make-interaction (fn [m] (= m "2")) "B" "A")
                                                   (make-interaction (fn [m] (= m "3")) "A" "B")
                                                   (make-interaction (fn [m] (= m "3")) "B" "A")]])
                                    (make-interaction (fn [m] (= m "4")) "A" "D")
                                    (make-interaction (fn [m] (= m "4")) "D" "A")
                                    (make-interaction (fn [m] (= m "5")) "A" ["B" "C" "D"])
                                    ])
                  (create-protocol [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                    (->interaction nil (fn [m] (= m "1")) "B" "A" #{} nil)
                                    (->branch nil [
                                                   [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                    (->interaction nil (fn [m] (= m "2")) "C" "A" #{} nil)
                                                    (->interaction nil (fn [m] (= m "3")) "A" "C" #{} nil)
                                                    (->interaction nil (fn [m] (= m "3")) "C" "A" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m "2")) "A" "B" #{} nil)
                                                    (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)
                                                    (->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                                                    (->interaction nil (fn [m] (= m "3")) "B" "A" #{} nil)]] nil)
                                    (->interaction nil (fn [m] (= m "4")) "A" "D" #{} nil)
                                    (->interaction nil (fn [m] (= m "4")) "D" "A" #{} nil)
                                    (->interaction nil (fn [m] (= m "5")) "A" ["B" "C" "D"] #{} nil)
                                    ])))
(def single-choice-multiple-interactions-protocolControl
  (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                 (->interaction nil (fn [m] (= m "1")) "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                                              (->interaction nil (fn [m] (= m "2")) "C" "A" #{}
                                                                             (->interaction nil (fn [m] (= m "3")) "A" "C" #{}
                                                                                            (->interaction nil (fn [m] (= m "3")) "C" "A" #{} (->interaction nil (fn [m] (= m "4")) "A" "D" #{}
                                                                                                                                              (->interaction nil (fn [m] (= m "4")) "D" "A" #{}
                                                                                                                                                             (->interaction nil (fn [m] (= m "5")) "A" ["B" "C" "D"] #{} nil)))))))
                                               (->interaction nil (fn [m] (= m "2")) "A" "B" #{}
                                                              (->interaction nil (fn [m] (= m "2")) "B" "A" #{}
                                                                             (->interaction nil (fn [m] (= m "3")) "A" "B" #{}
                                                                                            (->interaction nil (fn [m] (= m "3")) "B" "A" #{} (->interaction nil (fn [m] (= m "4")) "A" "D" #{}
                                                                                                                                              (->interaction nil (fn [m] (= m "4")) "D" "A" #{}
                                                                                                                                                             (->interaction nil (fn [m] (= m "5")) "A" ["B" "C" "D"] #{} nil)))))))] nil)
                                )))

(defn single-nested-choice-branch-protocol []
  (create-protocol [(make-choice [
                                  [(make-interaction (fn [m] (= m "1")) "A" "B")]
                                  [(make-choice [
                                                 [(make-interaction (fn [m] (= m "1")) "A" "C")]
                                                 [(make-interaction (fn [m] (= m "1")) "A" "D")]]
                                                )]]
                                 )
                    (make-interaction (fn [m] (= m "Done")) "A" "End")]))

(defn multiple-nested-choice-branch-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-choice [
                                                                 [(make-interaction (fn [m] (= m "1")) "A" "B")]
                                                                 [(make-interaction (fn [m] (= m "2")) "A" "B")]]
                                                                )]
                                                  [(make-choice [
                                                                 [(make-interaction (fn [m] (= m "3")) "A" "B")]
                                                                 [(make-interaction (fn [m] (= m "4")) "A" "B")]]
                                                                )]]
                                                 )])
                  (create-protocol [(->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil (fn [m] (= m "1"))"A" "B" #{} nil)]
                                                                   [(->interaction nil (fn [m] (= m "2")) "A" "B" #{} nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)]
                                                                   [(->interaction nil (fn [m] (= m "4")) "A" "B" #{} nil)]]
                                                              nil)]]
                                              nil)])))
(def multiple-nested-choice-branch-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                (->interaction nil (fn [m] (= m "2")) "A" "B" #{} nil)]
                           nil)
                 (->branch nil [
                                (->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                                (->interaction nil (fn [m] (= m "4")) "A" "B" #{} nil)]
                           nil)]
            nil))

(defn multiple-nested-branches-protocol [include-ids]
  (if include-ids (create-protocol [
                                    (make-choice [
                                                  [(make-choice [
                                                                 [(make-interaction (fn [m] (= m "1")) "A" "B")
                                                                  (make-interaction (fn [m] (= m "2")) "B" "A")]
                                                                 [(make-interaction (fn [m] (= m "1")) "A" "C")]]
                                                                )]
                                                  [(make-choice [
                                                                 [(make-choice [
                                                                                [(make-choice [
                                                                                               [(make-interaction (fn [m] (= m "1")) "A" "D")]
                                                                                               [(make-interaction (fn [m] (= m "1")) "A" ["E" "F" "G"])
                                                                                                (make-interaction (fn [m] (= m "3")) "F" "A")
                                                                                                (make-interaction (fn [m] (= m "4")) "G" "A")]]
                                                                                              )]
                                                                                [(make-interaction (fn [m] (= m "1")) "A" "H")]]
                                                                               )]
                                                                 [(make-interaction (fn [m] (= m "1")) "A" "I")]]
                                                                )]]
                                                 )
                                    (make-interaction (fn [m] (= m "Done")) "A" "End")]
                                   )
                  (create-protocol [
                                    (->branch nil [
                                                   [(->branch nil [
                                                                   [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                                                    (->interaction nil (fn [m] (= m "2")) "B" "A" #{} nil)]
                                                                   [(->interaction nil (fn [m] (= m "1")) "A" "C" #{} nil)]]
                                                              nil)]
                                                   [(->branch nil [
                                                                   [(->branch nil [
                                                                                   [(->branch nil [
                                                                                                   [(->interaction nil (fn [m] (= m "1")) "A" "D" #{} nil)]
                                                                                                   [(->interaction nil (fn [m] (= m "1")) "A" ["E" "F" "G"] #{} nil)
                                                                                                    (->interaction nil (fn [m] (= m "3")) "F" "A" #{} nil)
                                                                                                    (->interaction nil (fn [m] (= m "4")) "G" "A" #{} nil)]]
                                                                                              nil)]
                                                                                   [(->interaction nil (fn [m] (= m "1")) "A" "H" #{} nil)]]
                                                                              nil)]
                                                                   [(->interaction nil (fn [m] (= m "1")) "A" "I" #{} nil)]]
                                                              nil)]]
                                              nil)
                                    (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil)]
                                   )))

(def multiple-nested-branches-protocolControl
  (->branch nil [
                 (->branch nil [
                                (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                                               (->interaction nil (fn [m] (= m "2")) "B" "A" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil)))
                                (->interaction nil (fn [m] (= m "1")) "A" "C" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))]
                           nil)
                 (->branch nil [
                                (->branch nil [
                                               (->branch nil [
                                                              (->interaction nil (fn [m] (= m "1")) "A" "D" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))
                                                              (->interaction nil (fn [m] (= m "1")) "A" ["E" "F" "G"] #{}
                                                                             (->interaction nil (fn [m] (= m "3")) "F" "A" #{}
                                                                                            (->interaction nil (fn [m] (= m "4")) "G" "A" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))))]
                                                         nil)
                                               (->interaction nil (fn [m] (= m "1")) "A" "H" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))]
                                          nil)
                                (->interaction nil (fn [m] (= m "1")) "A" "I" #{} (->interaction nil (fn [m] (= m "Done")) "A" "End" #{} nil))]
                           nil)]
            nil)
  )


(defn single-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m "1")) "A" "B")
                                    (make-recursion :test [
                                                           (make-interaction (fn [m] (= m "1"))  "B" "A")
                                                           (make-choice [
                                                                         [(make-interaction (fn [m] (= m "2"))"A" "C")
                                                                          (make-interaction (fn [m] (= m "2"))"C" "A")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (fn [m] (= m "3")) "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    (make-interaction (fn [m] (= m "end")) "A" ["B" "C"])
                                    ])
                  (create-protocol [(->interaction nil (fn [m] (= m "1")) "A" "B" #{} nil)
                                    (->recursion nil :test [
                                                            (->interaction nil (fn [m] (= m "1")) "B" "A" #{} nil)
                                                            (->branch nil [
                                                                           [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                                            (->interaction nil (fn [m] (= m "2")) "C" "A" #{} nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)]] nil)
                                                            ] nil)
                                    (->interaction nil (fn [m] (= m "end" ))"A" ["B" "C"] #{} nil)
                                    ])))
(def single-recur-protocolControl
  (->interaction nil (fn [m] (= m "1")) "A" "B" #{}
                 (->interaction nil (fn [m] (= m "1")) "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                                              (->interaction nil (fn [m] (= m "2")) "C" "A" #{}
                                                                             (->recur-identifier nil :test :recur nil)))
                                               (->interaction nil (fn [m] (= m "3")) "A" "B" #{} (->interaction nil (fn [m] (= m "end")) "A" ["B" "C"] #{} nil))

                                               ] nil))
                 )
  )

(defn single-recur-one-choice-protocol []
  (create-protocol [(make-recursion :generate [
                                               (make-interaction (fn [m] (= m "1")) "A" "B")
                                               (make-choice [
                                                             [(make-interaction (fn [m] (= m "2")) "B" "A")
                                                              (do-recur :generate)]
                                                             [(make-interaction (fn [m] (= m "3")) "B" "A")]
                                                             ])
                                               ])
                    ]))

(defn one-recur-with-choice-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [
                                                                         [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (fn [m] (= m "3")) "A" "B")
                                                                          ]
                                                                         ])
                                                           ])
                                    ])
                  (create-protocol [(->recursion nil :test [
                                                            (->branch nil [
                                                                           [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                                            (->recur-identifier nil :test :recur nil)]
                                                                           [(->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                                                                            ]
                                                                           ] nil)
                                                            ] nil)
                                    ])
                  ))
(def one-recur-with-choice-protocolControl
  (->branch nil [
                 (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                (->recur-identifier nil :test :recur nil))
                 (->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                 ] nil))

(defn one-recur-with-startchoice-and-endchoice-protocol [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-recursion :test [
                                                                          (make-choice [
                                                                                        [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                                                         (do-recur :test)]
                                                                                        [(make-interaction (fn [m] (= m "3")) "A" "B")
                                                                                         ]
                                                                                        ])
                                                                          ])
                                                   ]
                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")]
                                                  ])
                                    ])
                  (create-protocol [(->branch nil [
                                                   [(->recursion nil :test [
                                                                            (->branch nil [
                                                                                           [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                                                            (->recur-identifier nil :test :recur nil)]
                                                                                           [(->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                                                                                            ]
                                                                                           ] nil)
                                                                            ] nil)
                                                    ]
                                                   [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)]
                                                   ] nil)
                                    ])))

(def one-recur-with-startchoice-and-endchoice-protocolControl
  (->branch nil [(->branch nil [(->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                               (->recur-identifier nil :test :recur nil))
                                (->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)
                                ] nil)
                 (->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                 ] nil))


(defn nested-recur-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-recursion :nested [
                                                                                    (make-interaction (fn [m] (= m "1")) "B" "A")
                                                                                    (make-choice [
                                                                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                                                                   (make-interaction (fn [m] (= m "2"))"C" "A")
                                                                                                   (do-recur :nested)]
                                                                                                  [(make-interaction (fn [m] (= m "3")) "A" "B")]
                                                                                                  ])
                                                                                    (make-choice [
                                                                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                                                                   (make-interaction (fn [m] (= m "2")) "C" "D")
                                                                                                   (do-recur :test)]
                                                                                                  [(make-interaction (fn [m] (= m "3")) "A" "E")]
                                                                                                  ])
                                                                                    ])]

                                                    )
                                    (make-interaction (fn [m] (= m "end")) "A" ["B" "C"])
                                    ])
                  (create-protocol [(->recursion nil :test [
                                                            (->recursion nil :nested [
                                                                                      (->interaction nil (fn [m] (= m "1")) "B" "A" #{} nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                                                                      (->interaction nil (fn [m] (= m "2")) "C" "A" #{} nil)
                                                                                                      (->recur-identifier nil :nested :recur nil)]
                                                                                                     [(->interaction nil (fn [m] (= m "3")) "A" "B" #{} nil)]
                                                                                                     ] nil)
                                                                                      (->branch nil [
                                                                                                     [(->interaction nil (fn [m] (= m "2")) "A" "C" #{} nil)
                                                                                                      (->interaction nil (fn [m] (= m "2")) "C" "D" #{} nil)
                                                                                                      (->recur-identifier nil :test :recur nil)]
                                                                                                     [(->interaction nil (fn [m] (= m "3")) "A" "E" #{} nil)]
                                                                                                     ] nil)
                                                                                      ] nil)]

                                                 nil)
                                    (->interaction nil (fn [m] (= m "end")) "A" ["B" "C"] #{} nil)])))
(def nested-recur-protocolControl
  (->interaction nil (fn [m] (= m "1")) "B" "A" #{}
                 (->branch nil [
                                (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                               (->interaction nil (fn [m] (= m "2")) "C" "A" #{}
                                                              (->recur-identifier nil :nested :recur nil)))
                                (->interaction nil (fn [m] (= m "3")) "A" "B" #{} (->branch nil [
                                                                                  (->interaction nil (fn [m] (= m "2")) "A" "C" #{}
                                                                                                 (->interaction nil (fn [m] (= m "2")) "C" "D" #{}
                                                                                                                (->recur-identifier nil :test :recur nil)))
                                                                                  (->interaction nil (fn [m] (= m "3")) "A" "E" #{} (->interaction nil (fn [m] (= m "end")) "A" ["B" "C"] #{} nil))
                                                                                  ] nil))
                                ] nil))
  )

(defn multiple-nested-recur-protocol []
  (create-protocol [(make-recursion :test [
                                           (make-recursion :nested [
                                                                    (make-choice [
                                                                                  [(make-recursion :nested-again [
                                                                                                                  (make-interaction (fn [m] (= m "2")) "A" "C")
                                                                                                                  (make-interaction (fn [m] (= m "2")) "C" "A")
                                                                                                                  (do-recur :nested-again)])]
                                                                                  [(make-interaction (fn [m] (= m "4")) "A" "B")
                                                                                   (do-recur :nested)]
                                                                                  [(make-interaction (fn [m] (= m "3")) "A" "D")]
                                                                                  ])
                                                                    (make-choice [
                                                                                  [(make-interaction (fn [m] (= m "2")) "A" "C")
                                                                                   (make-interaction (fn [m] (= m "2")) "C" "E")
                                                                                   (do-recur :test)]
                                                                                  [(make-interaction (fn [m] (= m "3")) "A" "F")]
                                                                                  ])
                                                                    ])]

                                    )
                    ]))

(defn two-buyer-protocol [include-ids]
  (if include-ids (create-protocol [(make-recursion :order-book [
                                                                 (make-interaction (fn [m] (= m "title")) "Buyer1" "Seller")
                                                                 (make-interaction (fn [m] (= m "quote")) "Seller" ["Buyer1" "Buyer2"])
                                                                 (make-interaction (fn [m] (= m "quoteDiv")) "Buyer1" "Buyer2")
                                                                 (make-choice [
                                                                               [(make-interaction (fn [m] (= m "ok")) "Buyer2" "Seller")
                                                                                (make-interaction (fn [m] (= m "date")) "Seller" "Buyer2")
                                                                                (do-recur :order-book)]
                                                                               [(make-interaction (fn [m] (= m "quit")) "Buyer2" "Seller")]])])
                                    ])
                  (create-protocol [(->recursion nil :order-book [
                                                                  (->interaction nil (fn [m] (= m "title")) "Buyer1" "Seller" #{} nil)
                                                                  (->interaction nil (fn [m] (= m "quote")) "Seller" ["Buyer1" "Buyer2"] #{} nil)
                                                                  (->interaction nil (fn [m] (= m "quoteDiv")) "Buyer1" "Buyer2" #{} nil)
                                                                  (->branch nil [
                                                                                 [(->interaction nil (fn [m] (= m "ok")) "Buyer2" "Seller" #{} nil)
                                                                                  (->interaction nil (fn [m] (= m "date")) "Seller" "Buyer2" #{} nil)
                                                                                  (->recur-identifier nil :order-book :recur nil)]
                                                                                 [(->interaction nil (fn [m] (= m "quit")) "Buyer2" "Seller" #{} nil)]] nil)] nil)
                                    ])))
(def two-buyer-protocolControl
  (->interaction nil (fn [m] (= m "title")) "Buyer1" "Seller" #{}
                 (->interaction nil (fn [m] (= m "quote")) "Seller" ["Buyer1" "Buyer2"] #{}
                                (->interaction nil (fn [m] (= m "quoteDiv")) "Buyer1" "Buyer2" #{}
                                               (->branch nil [
                                                              (->interaction nil (fn [m] (= m "ok")) "Buyer2" "Seller" #{}
                                                                             (->interaction nil (fn [m] (= m "date")) "Seller" "Buyer2" #{}
                                                                                            (->recur-identifier nil :order-book :recur nil)))
                                                              (->interaction nil (fn [m] (= m "quit")) "Buyer2" "Seller" #{} nil)] nil)))))
(defn parallel-after-interaction [include-ids]
  (if include-ids (create-protocol [(-->> (fn [m] (= m 1)) "a" "b")
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]
                                                    ])])
                  (create-protocol [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)])))

(def parallel-after-interactionControl
  (->interaction nil (fn [m] (= m 1)) "a" "b" #{}
                 (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                 (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                 ] nil)))

(defn parallel-after-interaction-with-after [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 1)) "a" "b")
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]
                                                    ])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(def parallel-after-interaction-with-afterControl
  (->interaction nil (fn [m] (= m 1)) "a" "b" #{}
                 (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                 (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                 ] (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil))))

(defn parallel-after-choice-with-after [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]
                                                  [(make-interaction (fn [m] (= m 0)) "a" "b")]])
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(def parallel-after-choice-with-afterControl
  (->branch nil [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ] (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)))
                 (->interaction nil (fn [m] (= m 0 ))"a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ] (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)))] nil))

(defn parallel-after-choice-with-after-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]
                                                  [(make-interaction (fn [m] (= m 0)) "a" "b")]])
                                    (make-parallel [[(make-interaction (fn [m] (= m 2))"b" "a")
                                                     (make-interaction (fn [m] (= m 3))"a" "b")]
                                                    [(make-interaction (fn [m] (= m 4))"b" "a")
                                                     (make-interaction (fn [m] (= m 5))"a" "b")]])
                                    (make-choice [[(make-interaction (fn [m] (= m 6)) "b" "a")]
                                                  [(make-interaction (fn [m] (= m 7)) "b" "a")]])])
                  (create-protocol [(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)]] nil)])))
(def parallel-after-choice-with-after-choiceControl
  (->branch nil [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2))"b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3))"a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ] (->branch nil [(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)
                                                                                   (->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)] nil)))
                 (->interaction nil (fn [m] (= m 0)) "a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ] (->branch nil [(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)
                                                                                   (->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)] nil)))] nil))

(defn parallel-after-rec-with-after [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (fn [m] (= m 0)) "a" "b")]])])
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-choice [[(make-interaction (fn [m] (= m 6)) "b" "a")]
                                                  [(make-interaction (fn [m] (= m 7)) "b" "a")]])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)]] nil)])))
(def parallel-after-rec-with-afterControl
  (->branch nil [(->interaction nil (fn [m] (= m 1))"a" "b" #{} (->recur-identifier nil :test :recur nil))
                 (->interaction nil (fn [m] (= m 0))"a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ] (->branch nil [(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)
                                                                                   (->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)] nil)))] nil))

(defn parallel-after-rec-with-after-rec [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (fn [m] (= m 1))"a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (fn [m] (= m 0)) "a" "b")]])])
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-recursion :test2 [
                                                            (make-choice [[(make-interaction (fn [m] (= m 6)) "b" "a")
                                                                           (do-recur :test2)]
                                                                          [(make-interaction (fn [m] (= m 7)) "b" "a")]])])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil (fn [m] (= m 0))"a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)]] nil)] nil)])))

(def parallel-after-rec-with-after-recControl
  (->branch nil [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} (->recur-identifier nil :test :recur nil))
                 (->interaction nil (fn [m] (= m 0)) "a" "b" #{} (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                 (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                                  ]
                                                             (->branch nil [(->interaction nil (fn [m] (= m 6)) "b" "a" #{}
                                                                                           (->recur-identifier nil :test2 :recur nil))
                                                                            (->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)] nil)))] nil))
(defn nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 1)) "a" "b")
                                    (make-parallel [[(make-parallel [[(make-interaction (fn [m] (= m "a")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "b")) "a" "b")]
                                                                     [(make-interaction (fn [m] (= m "b")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "a")) "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                                      (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                                     [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                                      (make-interaction (fn [m] (= m 5)) "a" "b")]])]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 1 ))"a" "b" #{} nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil (fn [m] (= m "a")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "b")) "a" "b" #{} nil)]
                                                                     [(->interaction nil (fn [m] (= m "b")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "a")) "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                                     [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]] nil)]] nil)])))

(def nested-parallelControl (->interaction nil (fn [m] (= m 1)) "a" "b" #{}
                                           (->lateral nil [(->lateral nil [(->interaction nil (fn [m] (= m "a")) "b" "a" #{}
                                                                                          (->interaction nil (fn [m] (= m "b")) "a" "b" #{} nil))
                                                                           (->interaction nil (fn [m] (= m "b")) "b" "a" #{}
                                                                                          (->interaction nil (fn [m] (= m "a")) "a" "b" #{} nil))] nil)
                                                           (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                          (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                           (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                          (->interaction nil (fn [m] (= m 5 ))"a" "b" #{} nil))] nil)] nil)))
(defn after-parallel-nested-parallel [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-interaction (fn [m] (= m 0)) "b" "a")
                                                     (make-interaction (fn [m] (= m 1)) "a" "b")]
                                                    [(make-interaction (fn [m] (= m "hi")) "b" "a")
                                                     (make-interaction (fn [m] (= m "hi")) "a" "b")]])
                                    (make-parallel [[(make-parallel [[(make-interaction (fn [m] (= m "a")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "b")) "a" "b")]
                                                                     [(make-interaction (fn [m] (= m "b")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "a")) "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                                      (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                                     [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                                      (make-interaction (fn [m] (= m 5)) "a" "b")]])]])])
                  (create-protocol [(->lateral nil [[(->interaction nil (fn [m] (= m 0))"b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 1))"a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m "hi")) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil (fn [m] (= m "a")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "b")) "a" "b" #{} nil)]
                                                                     [(->interaction nil (fn [m] (= m "b")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "a")) "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                                     [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]] nil)]] nil)])))
(def after-parallel-nested-parallelControl (->lateral nil [(->interaction nil (fn [m] (= m 0)) "b" "a" #{}
                                                                          (->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil))
                                                           (->interaction nil (fn [m] (= m "hi")) "b" "a" #{}
                                                                          (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil))]
                                                      (->lateral nil [(->lateral nil [(->interaction nil (fn [m] (= m "a")) "b" "a" #{}
                                                                                                     (->interaction nil (fn [m] (= m "b")) "a" "b" #{} nil))
                                                                                      (->interaction nil (fn [m] (= m "b")) "b" "a" #{}
                                                                                                     (->interaction nil (fn [m] (= m "a")) "a" "b" #{} nil))] nil)
                                                                      (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" "a" #{}
                                                                                                     (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil))
                                                                                      (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))] nil)] nil)))

(defn parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]
                                                                   [(make-interaction (fn [m] (= m 0)) "a" "b")]])]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]
                                                                    [(->interaction nil (fn [m] (= m 0))  "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(def parallel-with-choice-control
  (->lateral nil [(->branch nil [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                 (->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)] nil)
                  (->interaction nil (fn [m] (= m 4)) "b" "a" #{} (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                  ]
             (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)))

(defn parallel-with-choice-with-parallel [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [
                                                                   [(make-parallel [
                                                                                    [(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]
                                                                                                   [(make-interaction (fn [m] (= m 0)) "a" "b")]])]
                                                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])]
                                                                   [(make-interaction (fn [m] (= m 9)) "a" "b")]])]
                                                    [(make-interaction (fn [m] (= m "hi")) "b" "a")
                                                     (make-interaction (fn [m] (= m "hi")) "a" "b")]])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->lateral nil [[(->branch nil [[(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]
                                                                                                     [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)]] nil)]
                                                                                     [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                                                      (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]] nil)]
                                                                    [(->interaction nil (fn [m] (= m 9)) "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil (fn [m] (= m "hi")) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil)]] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(defn parallel-with-choice-with-parallelMulticast [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-choice [
                                                                   [(make-parallel [
                                                                                    [(make-choice [[(make-interaction (fn [m] (= m 1)) "a" ["b" "c"])]
                                                                                                   [(make-interaction (fn [m] (= m 0)) "a" ["b" "c"])]])]
                                                                                    [(make-interaction (fn [m] (= m 4)) "b" ["a" "c"])
                                                                                     (make-interaction (fn [m] (= m 5)) "a" ["b" "c"])]])]
                                                                   [(make-interaction (fn [m] (= m 9 ))"a" ["b" "c"])]])]
                                                    [(make-interaction (fn [m] (= m "hi")) "b" ["a" "c"])
                                                     (make-interaction (fn [m] (= m "hi")) "a" ["b" "c"])]])
                                    (make-interaction (fn [m] (= m 6 ))"b" ["a" "c"])])
                  (create-protocol [(->lateral nil [[(->branch nil [[(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]
                                                                                                     [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)]] nil)]
                                                                                     [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                                                      (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]] nil)]
                                                                    [(->interaction nil (fn [m] (= m 9)) "a" "b" #{} nil)]] nil)]
                                                    [(->interaction nil (fn [m] (= m "hi")) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil)]] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(def parallel-with-choice-with-parallel-control
  (->lateral nil [(->branch nil [(->lateral nil [(->branch nil [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                                                (->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)] nil)
                                                 (->interaction nil (fn [m] (= m 4)) "b" "a" #{} (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                                 ] nil)
                                 (->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)] nil)
                  (->interaction nil (fn [m] (= m "hi")) "b" "a" #{} (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil))
                  ] (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)))

(defn parallel-with-rec [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-recursion :test [(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]
                                                                                          [(make-interaction (fn [m] (= m 0)) "a" "b")
                                                                                           (do-recur :test)]])
                                                                            ])]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->lateral nil [[(->recursion nil :test [(->branch nil [[(->interaction nil (fn [m] (= m 1))"a" "b" #{} nil)]
                                                                                            [(->interaction nil (fn [m] (= m 0))"a" "b" #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)] nil)]
                                                    [(->interaction nil (fn [m] (= m 4))"b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5))"a" "b" #{} nil)]] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(def parallel-with-rec-control
  (->lateral nil [(->branch nil [(->interaction nil (fn [m] (= m 1))"a" "b" #{} nil)
                                 (->interaction nil (fn [m] (= m 0))"a" "b" #{}
                                                (->recur-identifier nil :test :recur nil))] nil)
                  (->interaction nil (fn [m] (= m 4 ))"b" "a" #{} (->interaction nil (fn [m] (= m 5 ))"a" "b" #{} nil))]
             (->interaction nil (fn [m] (= m 6 ))"b" "a" #{} nil)))

(defn rec-with-parallel-with-choice [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (fn [m] (= m 1))"a" "b")]
                                                                                          [(make-interaction (fn [m] (= m 0))"a" "b")
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                                            (make-interaction (fn [m] (= m 5)) "a" "b")]
                                                                           ])
                                                           ])
                                    (make-interaction (fn [m] (= m 6)) "b" "a")])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1))"a" "b" #{} nil)]
                                                                                            [(->interaction nil (fn [m] (= m 0))"a" "b" #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil (fn [m] (= m 4 ))"b" "a" #{} nil)
                                                                             (->interaction nil (fn [m] (= m 5 ))"a" "b" #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" "a" #{} nil)])))

(defn rec-with-parallel-with-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (fn [m] (= m 1))"a" ["b" "c"])]
                                                                                          [(make-interaction (fn [m] (= m 0))"a" ["b" "c"])
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (fn [m] (= m 4))"b" ["a" "c"])
                                                                            (make-interaction (fn [m] (= m 5))"a" ["b" "c"])]
                                                                           ])
                                                           ])
                                    (make-interaction (fn [m] (= m 6)) "b" ["a" "c"])])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" ["b" "c"] #{} nil)]
                                                                                            [(->interaction nil (fn [m] (= m 0)) "a" ["b" "c"] #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil (fn [m] (= m 4)) "b" ["a" "c"] #{} nil)
                                                                             (->interaction nil (fn [m] (= m 5)) "a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->interaction nil (fn [m] (= m 6)) "b" ["a" "c"] #{} nil)])))

(defn multiple-branches-choice [include-ids]
  (if include-ids (create-protocol [(make-choice [
                                                  [(make-interaction (fn [m] (= m 0)) "a" "b")
                                                   (make-interaction (fn [m] (= m 1)) "b" "a")]
                                                  [(make-interaction (fn [m] (= m 2)) "a" "b")
                                                   (make-interaction (fn [m] (= m 3)) "b" "a")]
                                                  [(make-interaction (fn [m] (= m 4)) "a" "b")
                                                   (make-interaction (fn [m] (= m 5)) "b" "a")]
                                                  ])
                                    ])
                  (create-protocol [(->branch nil [
                                                   [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                                    (->interaction nil (fn [m] (= m 1)) "b" "a" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 2)) "a" "b" #{} nil)
                                                    (->interaction nil (fn [m] (= m 3)) "b" "a" #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 4)) "a" "b" #{} nil)
                                                    (->interaction nil (fn [m] (= m 5)) "b" "a" #{} nil)]
                                                   ] nil)])))

(defn parallel-after-interaction-multicast [include-ids]
  (if include-ids (create-protocol [(-->> (fn [m] (= m 1)) "a" "b")
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" ["a" "c"])
                                                     (make-interaction (fn [m] (= m 3)) "a" ["b" "c"])]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]
                                                    ])
                                    (make-interaction (fn [m] (= m 6)) "b" ["a" "c"])])
                  (create-protocol [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" ["a" "c"] #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (make-interaction (fn [m] (= m 6 ))"b" ["a" "c"])])))

(def parallel-after-interaction-multicastControl
  (->interaction nil (fn [m] (= m 1)) "a" "b" #{}
                 (->lateral nil [(->interaction nil (fn [m] (= m 2)) "b" ["a" "c"] #{}
                                                (->interaction nil (fn [m] (= m 3)) "a" ["b" "c"] #{} nil))
                                 (->interaction nil (fn [m] (= m 4)) "b" "a" #{}
                                                (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil))
                                 ] (->interaction nil (fn [m] (= m 6)) "b" ["a" "c"] #{} nil))))

(defn parallel-after-choice-with-after-choice-multicast [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 1)) "a" "b")
                                    (make-parallel [[(make-interaction (fn [m] (= m 2))"b" ["a" "c"])
                                                     (make-interaction (fn [m] (= m 3))"a" ["b" "c"])]
                                                    [(make-interaction (fn [m] (= m 4))"b" "a")
                                                     (make-interaction (fn [m] (= m 5))"a" "b")]])
                                    (make-choice [[(make-interaction (fn [m] (= m 6)) "b" ["a" "c"])]
                                                  [(make-interaction (fn [m] (= m 7)) "b" "a")]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" ["a" "c"] #{} nil)
                                                     (->interaction nil (fn [m] (= m 3)) "a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4)) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5)) "a" "b" #{} nil)]
                                                    ] nil)
                                    (->branch nil [[(->interaction nil (fn [m] (= m 6)) "b" ["a" "c"] #{} nil)]
                                                   [(->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)]] nil)])))

(defn parallel-after-rec-with-after-rec-multicasts [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [
                                                           (make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")
                                                                          (do-recur :test)]
                                                                         [(make-interaction (fn [m] (= m 0)) "a" "b")]])])
                                    (make-parallel [[(make-interaction (fn [m] (= m 2)) "b" ["a" "c"])
                                                     (make-interaction (fn [m] (= m 3)) "a" ["b" "c"])]
                                                    [(make-interaction (fn [m] (= m 4)) "b" "a")
                                                     (make-interaction (fn [m] (= m 5)) "a" "b")]])
                                    (make-recursion :test2 [
                                                            (make-choice [[(make-interaction (fn [m] (= m 6)) "b" ["a" "c"])
                                                                           (do-recur :test2)]
                                                                          [(make-interaction (fn [m] (= m 7)) "b" "a")]])])])
                  (create-protocol [(->recursion nil :test
                                                 [(->branch nil [[(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                                                  (->recur-identifier nil :test :recur nil)]
                                                                 [(->interaction nil (fn [m] (= m 0 ))"a" "b" #{} nil)]] nil)] nil)
                                    (->lateral nil [[(->interaction nil (fn [m] (= m 2))"b" ["a" "c"] #{} nil)
                                                     (->interaction nil (fn [m] (= m 3))"a" ["b" "c"] #{} nil)]
                                                    [(->interaction nil (fn [m] (= m 4))"b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 5))"a" "b" #{} nil)]
                                                    ] nil)
                                    (->recursion nil :test2
                                                 [(->branch nil [[(->interaction nil (fn [m] (= m 6)) "b" ["a" "c"] #{} nil)
                                                                  (->recur-identifier nil :test2 :recur nil)]
                                                                 [(->interaction nil (fn [m] (= m 7)) "b" "a" #{} nil)]] nil)] nil)])))

(defn rec-with-parallel-with-choice-multicast-and-close [include-ids]
  (if include-ids (create-protocol [(make-recursion :test [(make-parallel [[(make-choice [[(make-interaction (fn [m] (= m 1)) "a" ["b" "c"])]
                                                                                          [(make-interaction (fn [m] (= m 0)) "a" ["b" "c"])
                                                                                           (do-recur :test)]])]
                                                                           [(make-interaction (fn [m] (= m 4)) "b" ["a" "c"])
                                                                            (make-interaction (fn [m] (= m 5)) "a" ["b" "c"])]
                                                                           ])
                                                           ])
                                    (make-closer "a" "b")
                                    (make-closer "a" "c")
                                    (make-interaction (fn [m] (= m 6)) "b" ["a" "c"])
                                    (make-closer "b" "a")
                                    (make-closer "b" "c")])
                  (create-protocol [(->recursion nil :test [(->lateral nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1))"a" ["b" "c"] #{} nil)]
                                                                                            [(->interaction nil (fn [m] (= m 0))"a" ["b" "c"] #{} nil)
                                                                                             (->recur-identifier nil :test :recur nil)]] nil)]
                                                                            [(->interaction nil (fn [m] (= m 4))"b" ["a" "c"] #{} nil)
                                                                             (->interaction nil (fn [m] (= m 5))"a" ["b" "c"] #{} nil)]
                                                                            ] nil)
                                                            ] nil)
                                    (->closer nil "a" "b" nil)
                                    (->closer nil "a" "c" nil)
                                    (->interaction nil (fn [m] (= m 6 ))"b" ["a" "c"] #{} nil)
                                    (->closer nil "b" "a" nil)
                                    (->closer nil "b" "c" nil)])))

(def rec-with-parallel-with-choice-multicast-and-closeControl
  (->lateral nil [(->branch nil [(->interaction nil (fn [m] (= m 1))"a" ["b" "c"] #{} nil)
                                 (->interaction nil (fn [m] (= m 0))"a" ["b" "c"] #{}
                                                (->recur-identifier nil :test :recur nil))] nil)
                  (->interaction nil (fn [m] (= m 4)) "b" ["a" "c"] #{}
                                 (->interaction nil (fn [m] (= m 5)) "a" ["b" "c"] #{} nil))
                  ]
             (->closer nil "a" "b"
                       (->closer nil "a" "c"
                                 (->interaction nil (fn [m] (= m 6)) "b" ["a" "c"] #{}
                                                (->closer nil "b" "a"
                                                          (->closer nil "b" "c" nil)))))))

(defn interaction-with-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0)) "a" "b")
                                    (make-closer "a" "b")])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->closer nil "a" "b" nil)])))

(defn interaction-with-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0)) "a" "b")
                                    (make-choice [[
                                                   (make-closer "a" "b")]
                                                  [(make-interaction (fn [m] (= m 1)) "a" "b")]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->branch nil [[(->closer nil "a" "b" nil)]
                                                   [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)]] nil)])))

(defn interaction-with-rec-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0)) "a" "b")
                                    (make-recursion :test [
                                                           (make-closer "a" "b")])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->recursion nil :test [(->closer nil "a" "b" nil)] nil)])))

(defn interaction-with-rec-and-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0)) "a" "b")
                                    (make-recursion :test
                                                    [(make-choice [[(make-closer "a" "b")]
                                                                   [(make-interaction (fn [m] (= m 1)) "a" "b")
                                                                    (do-recur :test)]])])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->recursion nil :test
                                                 [(->branch nil [[(->closer nil "a" "b" nil)]
                                                                 [(->interaction nil (fn [m] (= m 1)) "a" "b" #{} nil)
                                                                  (do-recur :test)]] nil)] nil)])))

(defn interaction-with-parallel-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0))"a" "b")
                                    (make-parallel [[(make-closer "a" "b")]
                                                    [(make-interaction (fn [m] (= m 1)) "a" "b")]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->lateral nil [[(->closer nil "a" "b" nil)]
                                                    [(->interaction nil (fn [m] (= m 1 ))"a" "b" #{} nil)]] nil)])))
(defn interaction-with-parallel-and-closer-with-interactions-in-parallel [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0)) "a" "b")
                                    (make-parallel [[(make-closer "a" "b")
                                                     (make-interaction (fn [m] (= m 2)) "b" "a")
                                                     (make-closer "b" "a")]
                                                    [(make-interaction (fn [m] (= m 1)) "a" "b")]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0))"a" "b" #{} nil)
                                    (->lateral nil [[(->closer nil "a" "b" nil)]
                                                    [(->interaction nil (fn [m] (= m 1 ))"a" "b" #{} nil)]] nil)])))

(defn interaction-with-nested-choice-and-closer [include-ids]
  (if include-ids (create-protocol [(make-interaction (fn [m] (= m 0 ))"a" "b")
                                    (make-choice [[(make-choice [[(make-interaction (fn [m] (= m 1)) "a" "b")]])]
                                                  [(make-choice [[(make-closer "a" "b")]])]])])
                  (create-protocol [(->interaction nil (fn [m] (= m 0)) "a" "b" #{} nil)
                                    (->branch nil [[(->branch nil [[(->interaction nil (fn [m] (= m 1 ))"a" "b" #{} nil)]] nil)]
                                                   [(->branch nil [[(->closer nil "a" "b" nil)]] nil)]] nil)])))

(defn after-parallel-nested-parallel-with-closer [include-ids]
  (if include-ids (create-protocol [(make-parallel [[(make-interaction (fn [m] (= m 0))"b" "a")
                                                     (make-interaction (fn [m] (= m 1))"a" "b")]
                                                    [(make-interaction (fn [m] (= m "hi")) "b" "a")
                                                     (make-interaction (fn [m] (= m "hi")) "a" "b")]])
                                    (make-parallel [[(make-parallel [[(make-interaction (fn [m] (= m "a")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "b")) "a" "b")]
                                                                     [(make-interaction (fn [m] (= m "b")) "b" "a")
                                                                      (make-interaction (fn [m] (= m "a")) "a" "b")]])]
                                                    [(make-parallel [[(make-interaction (fn [m] (= m 2)) "b" "a")
                                                                      (make-interaction (fn [m] (= m 3)) "a" "b")]
                                                                     [(make-closer "a" "b")
                                                                      (make-closer "b" "a")]])]])])
                  (create-protocol [(->lateral nil [[(->interaction nil (fn [m] (= m 0))"b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m 1))"a" "b" #{} nil)]
                                                    [(->interaction nil (fn [m] (= m "hi")) "b" "a" #{} nil)
                                                     (->interaction nil (fn [m] (= m "hi")) "a" "b" #{} nil)]] nil)
                                    (->lateral nil [[(->lateral nil [[(->interaction nil (fn [m] (= m "a")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "b")) "a" "b" #{} nil)]
                                                                     [(->interaction nil (fn [m] (= m "b")) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m "a")) "a" "b" #{} nil)]] nil)]
                                                    [(->lateral nil [[(->interaction nil (fn [m] (= m 2)) "b" "a" #{} nil)
                                                                      (->interaction nil (fn [m] (= m 3)) "a" "b" #{} nil)]
                                                                     [(->closer nil "a" "b" nil)
                                                                      (->closer nil "b" "a" nil)]] nil)]] nil)])))