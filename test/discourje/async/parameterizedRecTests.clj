(ns discourje.async.parameterizedRecTests
  (:require [clojure.test :refer :all]
            [discourje.core.async :refer :all]
            [discourje.async.protocolTestData :refer :all]
            [discourje.async.operationTests :refer :all]))

(defn single-recur-protocol-params [include-ids]
  (if include-ids (create-protocol [(make-interaction (message-checker "1") "A" "B")
                                    (make-recursion [:test {:r1 "A" :r2 "B" :r3 "C"}]
                                                    [(make-interaction (message-checker "1") :r2 :r1)
                                                     (make-choice [
                                                                   [(make-interaction (message-checker "2") :r1 :r3)
                                                                    (make-interaction (message-checker "2") :r3 :r1)
                                                                    (do-recur [:test {:r1 "A" :r2 "B" :r3 "C"}])]
                                                                   [(make-interaction (message-checker "3") :r1 :r2)
                                                                    ]
                                                                   ])
                                                     ])
                                    (make-interaction (message-checker "end") "A" ["B" "C"])
                                    ])
                  (create-protocol [(->interaction nil nil "A" "B" #{} nil)
                                    (->recursion nil [:test {:r1 "A" :r2 "B" :r3 "C"}]
                                                 [(->interaction nil nil "B" "A" #{} nil)
                                                  (->branch nil [
                                                                 [(->interaction nil nil "A" "C" #{} nil)
                                                                  (->interaction nil nil "C" "A" #{} nil)
                                                                  (->recur-identifier nil [:test {:r1 "A" :r2 "B" :r3 "C"}] :recur nil)]
                                                                 [(->interaction nil nil "A" "B" #{} nil)]] nil)
                                                  ] nil)
                                    (->interaction nil nil "A" ["B" "C"] #{} nil)
                                    ])))

(def single-recur-protocol-paramsControl
  (->interaction nil nil "A" "B" #{}
                 (->interaction nil nil "B" "A" #{}
                                (->branch nil [
                                               (->interaction nil nil "A" "C" #{}
                                                              (->interaction nil nil "C" "A" #{}
                                                                             (->recur-identifier nil [:test {:r1 "A" :r2 "B" :r3 "C"}] :recur nil)))
                                               (->interaction nil nil "A" "B" #{} (->interaction nil nil "A" ["B" "C"] #{} nil))

                                               ] nil))
                 )
  )

(deftest single-recur-protocol-params-test
  (let [mon (generate-monitor (single-recur-protocol-params false))]
    (is (= (get-active-interaction mon) single-recur-protocol-paramsControl))))

(deftest send-receive-single-recur-protocol-params
  (let [channels (generate-infrastructure (single-recur-protocol-params true))
        ab (get-channel channels "A" "B")
        ba (get-channel channels "B" "A")
        ac (get-channel channels "A" "C")
        ca (get-channel channels "C" "A")
        flag (atom false)]
    (do (>!! ab (->message "1" "AB"))
        (let [a->b (<!!-test ab)]
          (is (= "AB" a->b))
          (while (false? @flag)
            (>!! ba (->message "1" "AB"))
            (let [b->a (<!!-test ba)]
              (is (= "AB" b->a))
              (if (== 1 (+ 1 (rand-int 2)))
                (do
                  (>!! ac (->message "2" "AC"))
                  (let [a->c (<!!-test ac)]
                    (is (= "AC" a->c))
                    (>!! ca (->message "2" "AC"))
                    (let [c->a (<!!-test ca)]
                      (is (= "AC" c->a)))))
                (do
                  (>!! ab (->message "3" "AB3"))
                  (let [a->b3 (<!!-test ab)]
                    (is (= "AB3" a->b3))
                    (reset! flag true))))))
          (>!! [ab ac] (->message "end" "ending"))
          (let [a->b-end (<!!-test ab)
                a->c-end (<!!-test ac)]
            (is (= "ending" a->b-end))
            (is (= "ending" a->c-end)))))))